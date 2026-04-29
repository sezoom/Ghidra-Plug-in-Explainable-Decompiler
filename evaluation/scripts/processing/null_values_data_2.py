import pandas as pd

# Input and output paths
input_file = "results/processing/null_values_data.csv"
output_file = "results/processing/null_values_data_2.csv"

# Load dataset
df = pd.read_csv(input_file)

# Group and aggregate
grouped_df = df.groupby(
    ["test_type", "component_name", "file_size"], as_index=False
).agg(
    {
        "false_rate": "mean",
        "total_nb_of_tested": "mean",
        "ers_score": "mean",
        "control_attempts": "mean",
    }
)

# Fill NaN values using mean per test_type
cols_to_fill = ["false_rate", "total_nb_of_tested", "ers_score", "control_attempts"]

for col in cols_to_fill:
    grouped_df[col] = grouped_df.groupby("test_type")[col].transform(
        lambda x: x.fillna(x.mean())
    )

# Round numeric columns to 2 decimal places
grouped_df[cols_to_fill] = grouped_df[cols_to_fill].round(2)

# Print rows containing any NaN values after filling
nan_rows = grouped_df[grouped_df.isna().any(axis=1)]
print("Rows with NaN values in aggregated dataset after filling:")
print(nan_rows)

# Save result
grouped_df.to_csv(output_file, index=False)

print(f"Aggregated file saved to: {output_file}")
