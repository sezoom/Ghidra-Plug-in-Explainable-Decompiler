"""
Statistical Analysis Script for results_processed.csv
Outputs: results/plots/statistical_summary.txt

Sections:
  1. Weighted Mean False Rate (by test_type)
  2. Mann-Whitney U test (false_rate by test_type)
  3. Stats per test_type label
  4. Stats per component_name
  5. Stats per file_size
"""

import os

import numpy as np
import pandas as pd
from scipy import stats

# ── Paths ──────────────────────────────────────────────────────────────────────
INPUT_CSV = "results/results_processed.csv"
OUTPUT_DIR = "results/plots"
OUTPUT_TXT = os.path.join(OUTPUT_DIR, "statistical_summary.txt")

os.makedirs(OUTPUT_DIR, exist_ok=True)

# ── Load data ──────────────────────────────────────────────────────────────────
df = pd.read_csv(INPUT_CSV)

NUMERIC_COLS = ["false_rate", "total_nb_of_tested", "control_attempts"]

# Coerce numeric columns (handles any stray strings)
for col in NUMERIC_COLS:
    df[col] = pd.to_numeric(df[col], errors="coerce")


# ── Helper: compute stats for one group ───────────────────────────────────────
def group_stats(subset: pd.DataFrame, cols: list[str]) -> dict:
    """Return a dict of {col: {mean, median, q1, q3, min, max}} for each col."""
    result = {}
    for col in cols:
        s = subset[col].dropna()
        if s.empty:
            result[col] = {
                k: "N/A" for k in ("mean", "median", "q1", "q3", "min", "max")
            }
        else:
            result[col] = {
                "mean": round(s.mean(), 6),
                "median": round(s.median(), 6),
                "q1": round(s.quantile(0.25), 6),
                "q3": round(s.quantile(0.75), 6),
                "min": round(s.min(), 6),
                "max": round(s.max(), 6),
            }
    return result


def format_col_stats(col_name: str, s: dict, indent: str = "    ") -> list[str]:
    """Format one column's stats block into lines."""
    lines = [f"{indent}[ {col_name} ]"]
    lines.append(f"{indent}  Mean   : {s['mean']}")
    lines.append(f"{indent}  Median : {s['median']}")
    lines.append(f"{indent}  Q1     : {s['q1']}")
    lines.append(f"{indent}  Q3     : {s['q3']}")
    lines.append(f"{indent}  Min    : {s['min']}")
    lines.append(f"{indent}  Max    : {s['max']}")
    return lines


def section_header(title: str) -> list[str]:
    bar = "=" * 70
    return ["", bar, f"  {title}", bar]


def sub_header(title: str) -> list[str]:
    bar = "-" * 50
    return [f"  {title}", bar]


# ── Build report ───────────────────────────────────────────────────────────────
lines: list[str] = []
lines.append("STATISTICAL SUMMARY REPORT")
lines.append(f"Input file : {INPUT_CSV}")
lines.append(f"Rows loaded: {len(df)}")
lines.append(f"test_type labels: {sorted(df['test_type'].dropna().unique().tolist())}")

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 1 — Weighted Mean False Rate
# ══════════════════════════════════════════════════════════════════════════════
lines += section_header("SECTION 1 — Weighted Mean False Rate  (by test_type)")

labels = df["test_type"].dropna().unique()

if len(labels) < 2:
    lines.append("  WARNING: fewer than 2 test_type labels found.")
else:
    label_a, label_b = labels[0], labels[1]

    # For each test_type label (architecture), the weighted mean false rate is:
    #   r̄ = Σ(n_i · r_f_i) / Σ(n_i)
    # where r_f_i is the false_rate and n_i is total_nb_of_tested for case i.
    lines.append("  Formula: r̄ = Σ(n_i · r_f_i) / Σ(n_i)")
    lines.append("")
    for label in [label_a, label_b]:
        sub = df.loc[
            df["test_type"] == label, ["false_rate", "total_nb_of_tested"]
        ].dropna()
        total_n = sub["total_nb_of_tested"].sum()
        if total_n > 0:
            weighted_sum = (sub["false_rate"] * sub["total_nb_of_tested"]).sum()
            w_mean = weighted_sum / total_n
        else:
            weighted_sum = float("nan")
            w_mean = float("nan")
        lines.append(f"  test_type = '{label}'")
        lines.append(f"    Σ(n_i)              : {total_n}")
        lines.append(f"    Σ(n_i · r_f_i)      : {weighted_sum:.6f}")
        lines.append(f"    r̄ (weighted mean)   : {w_mean:.6f}")
        lines.append("")

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 2 — Mann-Whitney U test
# ══════════════════════════════════════════════════════════════════════════════
lines += section_header("SECTION 2 — Mann-Whitney U Test  (false_rate by test_type)")

if len(labels) < 2:
    lines.append("  WARNING: fewer than 2 test_type labels found — cannot run test.")
else:
    group_a = df.loc[df["test_type"] == label_a, "false_rate"].dropna()
    group_b = df.loc[df["test_type"] == label_b, "false_rate"].dropna()

    stat, p_value = stats.mannwhitneyu(group_a, group_b, alternative="two-sided")

    lines.append(f"  Comparing '{label_a}'  vs  '{label_b}'  on  false_rate")
    lines.append(f"  n ('{label_a}') = {len(group_a)}")
    lines.append(f"  n ('{label_b}') = {len(group_b)}")
    lines.append(f"")
    lines.append(f"  U statistic : {stat}")
    lines.append(f"  p-value     : {p_value:.6e}")
    lines.append(f"")
    alpha = 0.05
    if p_value < alpha:
        lines.append(f"  Result : Statistically SIGNIFICANT difference (α={alpha})")
        lines.append(f"           The two test_type groups differ in false_rate.")
    else:
        lines.append(f"  Result : No statistically significant difference (α={alpha})")
        lines.append(f"           Cannot reject H₀ of equal distributions.")

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 3 — Stats per test_type label
# ══════════════════════════════════════════════════════════════════════════════
lines += section_header("SECTION 3 — Statistics per test_type label")

for label in sorted(df["test_type"].dropna().unique()):
    subset = df[df["test_type"] == label]
    lines += sub_header(f"test_type = '{label}'  (n={len(subset)})")
    s = group_stats(subset, NUMERIC_COLS)
    for col in NUMERIC_COLS:
        lines += format_col_stats(col, s[col])
    lines.append("")

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 4 — Stats per component_name
# ══════════════════════════════════════════════════════════════════════════════
lines += section_header("SECTION 4 — Statistics per component_name")

for comp in sorted(df["component_name"].dropna().unique()):
    subset = df[df["component_name"] == comp]
    lines += sub_header(f"component_name = '{comp}'  (n={len(subset)})")
    s = group_stats(subset, NUMERIC_COLS)
    for col in NUMERIC_COLS:
        lines += format_col_stats(col, s[col])
    lines.append("")

# ══════════════════════════════════════════════════════════════════════════════
# SECTION 5 — Stats per file_size
# ══════════════════════════════════════════════════════════════════════════════
lines += section_header("SECTION 5 — Statistics per file_size")

for fsize in sorted(df["file_size"].dropna().unique()):
    subset = df[df["file_size"] == fsize]
    lines += sub_header(f"file_size = '{fsize}'  (n={len(subset)})")
    s = group_stats(subset, NUMERIC_COLS)
    for col in NUMERIC_COLS:
        lines += format_col_stats(col, s[col])
    lines.append("")

# ── Write output ───────────────────────────────────────────────────────────────
report = "\n".join(lines)

with open(OUTPUT_TXT, "w", encoding="utf-8") as f:
    f.write(report)

print(f"Report written to: {OUTPUT_TXT}")
print(f"Total lines: {len(lines)}")
