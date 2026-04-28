import csv
import json
import os
from collections import Counter

INPUT_PATH = "./results/results.json"
OUTPUT_PATH = "./results/results.csv"

MU_0 = 0.0858
K = 9


def json_to_csv(input_path: str, output_path: str) -> None:
    with open(input_path, "r", encoding="utf-8") as f:
        data = json.load(f)

    os.makedirs(os.path.dirname(output_path), exist_ok=True)

    fieldnames = [
        "id",
        "test_type",
        "component_name",
        "file_name",
        "file_size",
        "false_rate",
        "total_nb_of_tested",
        "control_attempts",
        "errors_functions",
        "errors_variables",
        "errors_calls",
    ]

    test_type_counter = Counter()
    component_name_counter = Counter()
    file_size_counter = Counter()

    with open(output_path, "w", newline="", encoding="utf-8") as f:
        writer = csv.DictWriter(f, fieldnames=fieldnames)
        writer.writeheader()

        for key, entry in data.items():
            errors = entry.get("errors", {})
            file_name = entry.get("file_name", "")
            parts = file_name.split("_", 1)
            file_size = parts[1] if len(parts) > 1 else ""

            test_type = entry.get("test_type", "")
            component_name = entry.get("component_name", "")
            false_rate = entry.get("false_rate", "")
            total_nb_of_tested = entry.get("total_nb_of_tested", "")

            test_type_counter[test_type] += 1
            component_name_counter[component_name] += 1
            file_size_counter[file_size] += 1

            writer.writerow(
                {
                    "id": key,
                    "test_type": test_type,
                    "component_name": component_name,
                    "file_name": file_name,
                    "file_size": file_size,
                    "false_rate": false_rate,
                    "total_nb_of_tested": total_nb_of_tested,
                    "control_attempts": entry.get("control_attempts", ""),
                    "errors_functions": "|".join(errors.get("functions", [])),
                    "errors_variables": "|".join(errors.get("variables", [])),
                    "errors_calls": "|".join(errors.get("calls", [])),
                }
            )

    total = len(data)
    print(f"Done — wrote {total} rows to {output_path}")
    print(f"\nNb total of rows: {total}")
    print("\ntest_type:")
    for label, count in sorted(test_type_counter.items()):
        print(f"  {label!r}: {count}")
    print("\ncomponent_name:")
    for label, count in sorted(component_name_counter.items()):
        print(f"  {label!r}: {count}")
    print("\nfile_size:")
    for label, count in sorted(file_size_counter.items()):
        print(f"  {label!r}: {count}")


if __name__ == "__main__":
    json_to_csv(INPUT_PATH, OUTPUT_PATH)
