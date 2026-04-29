import os

import pandas as pd

# ── Load datasets ──────────────────────────────────────────────────────────────
df1 = pd.read_csv("results/processing/null_values_data.csv")
df2 = pd.read_csv("results/processing/null_values_data_2.csv")

# ── Fill null values ───────────────────────────────────────────────────────────
# Key columns used to match rows between the two datasets
key_cols = ["test_type", "component_name", "file_size"]

# Columns that can be filled (all non-key columns present in both datasets)
fillable_cols = [
    c
    for c in df1.columns
    if c not in key_cols and c != "file_name" and c in df2.columns
]

# Build a lookup dict from df2: (test_type, component_name, file_size) -> row
df2_indexed = df2.set_index(key_cols)

filled_counts = {col: 0 for col in fillable_cols}

for idx, row in df1.iterrows():
    key = (row["test_type"], row["component_name"], row["file_size"])
    for col in fillable_cols:
        if pd.isnull(row[col]):
            try:
                fill_val = df2_indexed.loc[key, col]
                # If multiple rows match, take the first value
                if isinstance(fill_val, pd.Series):
                    fill_val = fill_val.iloc[0]
                df1.at[idx, col] = fill_val
                filled_counts[col] += 1
            except KeyError:
                pass  # No matching row in df2 — leave as null

print("\nValues filled per column:")
for col, count in filled_counts.items():
    print(f"  {col}: {count} cell(s) filled")

# ── Stats on FINAL dataset ─────────────────────────────────────────────────────
print("\n" + "=" * 60)
print("FINAL DATASET STATISTICS")
print("=" * 60)

total_nulls = df1.isnull().sum().sum()
print(f"\nTotal null values remaining: {total_nulls}")
null_per_col = df1.isnull().sum()
null_per_col = null_per_col[null_per_col > 0]
if not null_per_col.empty:
    print("Null values per column:")
    for col, count in null_per_col.items():
        print(f"  {col}: {count}")

print(f"\nTotal number of rows : {len(df1)}")

print("\nRows per test_type:")
for val, count in df1["test_type"].value_counts().sort_index().items():
    print(f"  {val}: {count}")

print("\nRows per component_name:")
for val, count in df1["component_name"].value_counts().sort_index().items():
    print(f"  {val}: {count}")

print("\nRows per file_size:")
for val, count in df1["file_size"].value_counts().sort_index().items():
    print(f"  {val}: {count}")

# ── Save result ────────────────────────────────────────────────────────────────
os.makedirs("results", exist_ok=True)
output_path = "results/results_processed.csv"
df1.to_csv(output_path, index=False)
print(f"\nProcessed file saved to: {output_path}")
print("=" * 60)
