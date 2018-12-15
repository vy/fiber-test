#!/usr/bin/env python
# coding=utf-8


import glob
import json
import logging
import os
import re
import string
import subprocess
import time
import xml.etree.ElementTree as XmlElementTree


JVM_ARGS = ["-Xms512M", "-Xmx512M", "--add-opens", "java.base/java.lang=ALL-UNNAMED"]

JMH_FORK_COUNT = os.getenv("jmh.forkCount", "3")

JMH_ITERATION_COUNT = os.getenv("jmh.iterationCount", "5")

JMH_WARMUP_ITERATION_COUNT = os.getenv("jmh.warmupIterationCount", "5")

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s %(levelname)7s [%(name)s] %(message)s",
    datefmt="%Y-%m-%d %H:%M:%S")

LOGGER = logging.getLogger(__name__)

JAVA_FILEPATH = os.path.join(os.getenv("JAVA_HOME"), "bin", "java") if os.getenv("JAVA_HOME") else "java"

BENCHMARK_DIR = os.path.dirname(os.path.realpath(__file__))

PROJECT_DIR = os.path.abspath(os.path.join(BENCHMARK_DIR, ".."))


def require_filepath(filepath):
    if not os.path.exists(filepath):
        raise Exception("could not find file: {}".format(filepath))
    if not os.path.isfile(filepath):
        raise Exception("was expecting a file: {}".format(filepath))
    if not os.access(filepath, os.R_OK):
        raise Exception("file is not readable: {}".format(filepath))
    return filepath


def get_project_artifact_id():
    pom_filepath = os.path.join(PROJECT_DIR, "pom.xml")
    tree = XmlElementTree.parse(pom_filepath)
    return tree.findall('./{http://maven.apache.org/POM/4.0.0}artifactId')[0].text


def get_project_jar_filepath():
    artifact_id = get_project_artifact_id()
    filename = "{}.jar".format(artifact_id)
    filepath = os.path.join(PROJECT_DIR, "target", filename)
    return require_filepath(filepath)


def get_quasar_jar_filepath():
    filepath = os.path.join(PROJECT_DIR, "target", "agents", "quasar-core.jar")
    return require_filepath(filepath)


def get_text_output_filepath(config):
    return os.path.join(BENCHMARK_DIR, "results-{}-{}.txt".format(config["name"], config["type"]))


def get_json_output_filepath(config):
    return os.path.join(BENCHMARK_DIR, "results-{}-{}.json".format(config["name"], config["type"]))


def run_benchmark(config):
    LOGGER.info("starting benchmark (name={}, type={})".format(config["name"], config["type"]))
    start_instant_seconds = time.time()
    text_output_filepath = get_text_output_filepath(config)
    jvm_args = " ".join(JVM_ARGS + config["jvm_args"])
    json_output_filepath = get_json_output_filepath(config)
    java_filepath = config.get("java_filepath", JAVA_FILEPATH)
    popen_cmd = [
        java_filepath,
        "-jar", config["jar_filepath"],
        "-jvmArgsAppend", jvm_args,
        "-wi", JMH_WARMUP_ITERATION_COUNT,
        "-i", JMH_ITERATION_COUNT,
        "-f", JMH_FORK_COUNT,
        "-bm", "thrpt",
        "-tu", "s",
        "-rf", "JSON",
        "-rff", json_output_filepath,
        "-prof", "stack",
        "-prof", "gc",
        "-v", "EXTRA",
        config["jmh_class_pattern"]]
    with open(text_output_filepath, "w") as text_output_filepath_handle:
        popen = subprocess.Popen(
            popen_cmd,
            bufsize=1,
            cwd=PROJECT_DIR,
            stderr=subprocess.STDOUT,
            stdout=text_output_filepath_handle)
        popen.communicate()
        return_code = popen.returncode
        if return_code != 0:
            raise Exception(
                "benchmark failure (name={}, type={}, return_code={})".format(
                    config["name"], config["type"], return_code))
    stop_instant_seconds = time.time()
    total_duration_seconds = stop_instant_seconds - start_instant_seconds
    LOGGER.info(
        "completed benchmark (name={}, type={}, total_duration_seconds={:.0f})".format(
            config["name"], config["type"], total_duration_seconds))


def run_benchmarks(configs):
    LOGGER.info("starting benchmarks")
    start_instant_seconds = time.time()
    for config in configs:
        run_benchmark(config)
    stop_instant_seconds = time.time()
    total_duration_seconds = stop_instant_seconds - start_instant_seconds
    LOGGER.info("completed benchmarks (total_duration_seconds={:.0f})".format(total_duration_seconds))


def get_host_kernel_release():
    return subprocess.check_output(["uname", "-r"])


def get_host_arch():
    return subprocess.check_output(["uname", "-i"])


def get_host_cpu():
    lines = open("/proc/cpuinfo").readlines()
    cpu_count = len([line for line in lines if re.match("^processor\s+: [0-9]+\s*$", line)])
    cpu_model = None
    for line in lines:
        match = re.match("^model name\s+: (.+)\s*$", line)
        if match:
            cpu_model = match.groups()[0]
            break
    return "{} x {}".format(cpu_count, cpu_model)


def get_common_jvm_args_by_type(configs):

    # Determine config count by type.
    config_count_by_type = {}
    for config in configs:
        type = config["type"]
        config_count_by_type[type] = config_count_by_type.get(type, 0) + 1

    # Collect JVM argument frequencies by type.
    freq_by_jvm_arg_by_type = {}
    for config in configs:
        type = config["type"]
        jvm_args = config.get("jvm_args", [])
        freq_by_jvm_arg = freq_by_jvm_arg_by_type.get(type, {})
        for jvm_arg in jvm_args:
            freq_by_jvm_arg[jvm_arg] = freq_by_jvm_arg.get(jvm_arg, 0) + 1
        freq_by_jvm_arg_by_type[type] = freq_by_jvm_arg

    # Filter common JVM arguments.
    common_jvm_args_by_type = {}
    for type, freq_by_jvm_arg in freq_by_jvm_arg_by_type.items():
        for jvm_arg, freq in freq_by_jvm_arg.items():
            if freq >= config_count_by_type[type]:
                common_jvm_args_by_type[type] = common_jvm_args_by_type.get(type, []) + [jvm_arg]
    return common_jvm_args_by_type


def get_java_version(config):
    java_filepath = config.get("java_filepath", JAVA_FILEPATH)
    output = subprocess.check_output([java_filepath, "-version"], stderr=subprocess.STDOUT)
    return str(output).split('"')[1]


def read_results_by_type(configs):

    # Collect results.
    results_by_type = {}
    for config in configs:
        type = config["type"]
        java_version = get_java_version(config)
        json_output_filepath = get_json_output_filepath(config)
        json_output_instant = os.path.getmtime(json_output_filepath)
        json_output = open(json_output_filepath).read()
        json_dicts = json.loads(json_output)
        for json_dict in json_dicts:
            result = {
                "java_version": java_version,
                "instant": json_output_instant,
                "benchmark": json_dict["benchmark"],
                "op_rate": json_dict["primaryMetric"]["scorePercentiles"]["99.0"],
                "gc_rate": json_dict["secondaryMetrics"][u"·gc.alloc.rate"]["scorePercentiles"]["99.0"]}
            results = results_by_type.get(type, [])
            results.append(result)
            results_by_type[type] = results

    # Enrich results with normalized op rate slowdown.
    for type, results in results_by_type.items():
        max_op_rate = max([result["op_rate"] for result in results])
        for result in results:
            result["op_rate_norm"] = result["op_rate"] / max_op_rate

    # Sort results.
    for results in results_by_type.values():
        results.sort(key=lambda result: result["op_rate"], reverse=True)

    # Return grouped results.
    return results_by_type


def plot_results(configs):
    html_file = os.path.join(BENCHMARK_DIR, "results.html")
    with open(html_file, "w") as html_file_handle:

        # Write header.
        html_file_handle.write("""
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="utf-8">
    <title>log4j2-logstash-layout Benchmark Results</title>
</head>
<body>
    <style>
        #results th, #results td { padding: 0.3em }
        #results .op_rate, #results .gc_rate { text-align: right }
        #results th { background-color: #cfcfcf }
        #results #footnotes { font-size: 0.8em; }
    </style>
    <div id="results">""")

        # Write settings.
        html_file_handle.write("""
        <h1>settings</h1>
        <ul>""")
        for setting_key, setting_val in [
            ("kernel", get_host_kernel_release()),
            ("arch", get_host_arch()),
            ("CPU", get_host_cpu()),
            ("JVM_ARGS", " ".join(JVM_ARGS)),
            ("JMH_FORK_COUNT", JMH_FORK_COUNT),
            ("JMH_ITERATION_COUNT", JMH_ITERATION_COUNT),
            ("JMH_WARMUP_ITERATION_COUNT", JMH_WARMUP_ITERATION_COUNT)]:
            html_file_handle.write("""
            <li>{}: {}</li>""".format(setting_key, setting_val))
        html_file_handle.write("""
        </ul>""")

        # Write results.
        common_jvm_args_by_type = get_common_jvm_args_by_type(configs)
        results_by_type = read_results_by_type(configs)
        for type, results in results_by_type.items():
            html_file_handle.write("""
        <h1>{}</h1>
        <p>JVM_ARGS: {}</p>
        <table>
            <thead>
                <tr>
                    <th>Benchmark</th>
                    <th>Java Version</th>
                    <th>Instant</th>
                    <th colspan="2">ops/sec<sup>*</sup></th>
                    <th>MB/sec<sup>*</sup></th>
                </tr>
            </thead>
            <tbody>""".format(
                type,
                " ".join(common_jvm_args_by_type.get(type, []))))
            for result in results:
                benchmark_name = result["benchmark"].split(".")[-2]
                html_file_handle.write(
                    """
                <tr data-benchmark="{}">
                    <td class="benchmark">{}</td>
                    <td class="java_version">{}</td>
                    <td class="instant">{}</td>
                    <td class="op_rate">{}</td>
                    <td class="op_rate_bar">{}</td>
                    <td class="gc_rate">{}</td>
                </tr>""".format(
                        benchmark_name,
                        benchmark_name,
                        result["java_version"],
                        time.strftime("%Y-%m-%d", time.localtime(result["instant"])),
                        "{:,.1f}".format(result["op_rate"] * 1e3),
                        ("▉" * (1 + int(19 * result["op_rate_norm"]))) + (" ({:.0f}%)".format(100 * result["op_rate_norm"])),
                        "{:,.1f}".format(result["gc_rate"]) if result["gc_rate"] > 0 else "N/A"))
            html_file_handle.write("""
            </tbody>
        </table>""")

        # Write footer.
        html_file_handle.write("""
        <p id="footnotes">
            <sup>*</sup> 99<sup>th</sup> percentile
        </p>
    </div>
</body>
""")


def collect_configs():
    LOGGER.info("collecting benchmark configuration")
    configs = []
    glob_pattern = os.path.join(PROJECT_DIR, "*", "benchmark-config.json")
    for config_filepath in glob.glob(glob_pattern):
        module_filepath = os.path.dirname(config_filepath)
        config_json_template_text = open(config_filepath).read()
        config_json_template = string.Template(config_json_template_text)
        config_json = config_json_template.substitute(pwd=module_filepath)
        config = json.loads(config_json)
        configs.append(config)
    config_names = [config["name"] for config in configs]
    LOGGER.info("collected benchmark configuration (names={})".format(config_names))
    return configs


def extend_configs_for_ring_benchmark(configs):
    extended_configs = []
    jvm_args_extensions_by_benchmark_type = {
        "ring-start-stop": ["-Dring.workerCount=1000", "-Dring.messagePassingCount=1"],
        "ring-ctx-switch": ["-Dring.workerCount=100", "-Dring.messagePassingCount=1000000"]}
    for config in configs:
        for benchmark_type, jvm_args_extension in jvm_args_extensions_by_benchmark_type.items():
            extended_config = dict(config)
            extended_config["jvm_args"] = config.get("jvm_args", []) + jvm_args_extension
            extended_config["type"] = benchmark_type
            extended_config["jmh_class_pattern"] = "com.vlkan.fibertest.*RingBenchmark.*"
            extended_configs.append(extended_config)
    return extended_configs


def extend_configs_for_graal(configs):
    extended_configs = []
    for config in configs:
        extended_config = dict(config)
        extended_config["jvm_args"] = \
            config.get("jvm_args", []) + \
                ["-XX:+UnlockExperimentalVMOptions", "-XX:+UseJVMCICompiler"]
        extended_config["type"] += "-graal"
        extended_configs.append(config)
        extended_configs.append(extended_config)
    return extended_configs


def extend_configs(configs):
    configs = extend_configs_for_ring_benchmark(configs)
    configs = extend_configs_for_graal(configs)
    return configs


def main():
    configs = collect_configs()
    extended_configs = extend_configs(configs)
    run_benchmarks(extended_configs)
    plot_results(extended_configs)


if __name__ == "__main__":
    main()
