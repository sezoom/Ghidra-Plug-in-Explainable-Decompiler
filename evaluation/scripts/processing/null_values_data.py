import pandas as pd


def main():
    input_path = "./results/results.csv"
    output_path = "./results/processing/null_values_data.csv"

    df = pd.read_csv(input_path)

    cols_to_remove = ["errors_functions", "errors_variables", "errors_calls"]
    df = df.drop(
        columns=[c for c in cols_to_remove if c in df.columns], errors="ignore"
    )

    grouped = df.groupby(
        ["test_type", "component_name", "file_size", "file_name"], as_index=False
    ).agg(
        false_rate=("false_rate", "mean"),
        total_nb_of_tested=("total_nb_of_tested", "mean"),
        ers_score=("ers_score", "mean"),
        control_attempts=("control_attempts", "mean"),
    )

    # Round numeric columns to 2 decimal places
    grouped = grouped.round(2)

    print(f"Number of rows: {len(grouped)}")

    grouped.to_csv(output_path, index=False)
    print(f"Saved aggregated CSV to: {output_path}")


if __name__ == "__main__":
    main()
