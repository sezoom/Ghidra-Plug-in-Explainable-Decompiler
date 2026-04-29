"""
generate_plots.py
-----------------
Reads  results/results_processed.csv  and writes 5 publication-quality plots
to  results/plots/.

Expected CSV columns:
    test_type, component_name, file_size, file_name,
    false_rate, total_nb_of_tested, control_attempts
"""

import os
import warnings

import matplotlib.patches as mpatches
import matplotlib.pyplot as plt
import numpy as np
import pandas as pd
import seaborn as sns
from matplotlib.ticker import MultipleLocator

warnings.filterwarnings("ignore")

# ── Paths ──────────────────────────────────────────────────────────────────────
INPUT_CSV = os.path.join("results", "results_processed.csv")
OUTPUT_DIR = os.path.join("results", "plots", "false_rate")
os.makedirs(OUTPUT_DIR, exist_ok=True)

# ── Load data ──────────────────────────────────────────────────────────────────
df = pd.read_csv(INPUT_CSV)
df["false_rate"] = pd.to_numeric(df["false_rate"], errors="coerce")
df["total_nb_of_tested"] = pd.to_numeric(df["total_nb_of_tested"], errors="coerce")

# ── Global style ───────────────────────────────────────────────────────────────
PALETTE_2 = ["#4C72B0", "#DD8452"]  # two test-types
PALETTE_4 = sns.color_palette("tab10", 4)  # four components
PALETTE_5 = sns.color_palette("Set2", 5)  # five file sizes

FILE_SIZE_ORDER = ["0_50", "50_100", "100_200", "200_300", "300_400"]

plt.rcParams.update(
    {
        "figure.dpi": 150,
        "font.family": "DejaVu Sans",
        "axes.spines.top": False,
        "axes.spines.right": False,
        "axes.grid": True,
        "grid.alpha": 0.35,
        "grid.linestyle": "--",
    }
)

test_types = sorted(df["test_type"].unique())


# ══════════════════════════════════════════════════════════════════════════════
# Plot 1 — False Rate by Test Type  +  mean(total_nb_of_tested) side panels
# ══════════════════════════════════════════════════════════════════════════════
def plot1():
    n_types = len(test_types)
    fig, axes = plt.subplots(1, len(test_types), figsize=(10, 6), sharey=True)
    if len(test_types) == 1:
        axes = [axes]
    fig.suptitle(
        "False Rate Distribution by Test Type\nwith Average Number of Tests Performed",
        fontsize=14,
        fontweight="bold",
        y=1.01,
    )

    # ── Box plots ──────────────────────────────────────────────────────────────
    for i, (ax, tt) in enumerate(zip(axes, test_types)):
        data = df.loc[df["test_type"] == tt, "false_rate"].dropna()

        bp = ax.boxplot(
            data,
            patch_artist=True,
            widths=0.45,
            medianprops=dict(color="white", linewidth=2.5),
            whiskerprops=dict(linewidth=1.4),
            capprops=dict(linewidth=1.4),
            flierprops=dict(
                marker="o",
                markerfacecolor=PALETTE_2[i],
                markersize=5,
                alpha=0.5,
                linestyle="none",
            ),
        )
        bp["boxes"][0].set_facecolor(PALETTE_2[i])
        bp["boxes"][0].set_alpha(0.82)

        mean_val = data.mean()
        ax.axhline(
            mean_val,
            color=PALETTE_2[i],
            linewidth=1.5,
            linestyle=":",
            alpha=0.9,
            label=f"Mean FR = {mean_val:.3f}",
        )

        ax.set_ylim(-0.05, 1.05)
        ax.yaxis.set_major_locator(MultipleLocator(0.2))
        ax.set_xticks([1])
        ax.set_xticklabels([tt.upper()], fontsize=11, fontweight="bold")
        ax.legend(fontsize=8, loc="upper right")

        if i == 0:
            ax.set_ylabel("False Rate", fontsize=11)
        else:
            ax.tick_params(labelleft=False, left=False)
            ax.spines["left"].set_visible(False)

        # ── Badge: mean total_nb_of_tested — drawn directly on the box axis ──
        mean_n = df.loc[df["test_type"] == tt, "total_nb_of_tested"].mean()
        ax.text(
            1,  # x in data coords (centre of the single box)
            0.80,  # y in data coords  → sits near the 0.8 gridline
            f"avg tests\nn̄ = {mean_n:.1f}",
            ha="center",
            va="center",
            fontsize=9,
            color=PALETTE_2[i],
            fontweight="bold",
            zorder=10,  # always on top
            bbox=dict(
                boxstyle="round,pad=0.45",
                facecolor="white",
                edgecolor=PALETTE_2[i],
                linewidth=1.8,
                alpha=0.95,
            ),
        )

    fig.tight_layout()
    fig.savefig(
        os.path.join(OUTPUT_DIR, "plot1_falserate_by_testtype.png"), bbox_inches="tight"
    )
    plt.close(fig)
    print("✔  Plot 1 saved.")


# ══════════════════════════════════════════════════════════════════════════════
# Plot 2 — False Rate by Test Type × Component Name  (grouped box plot)
# ══════════════════════════════════════════════════════════════════════════════
def plot2():
    components = sorted(df["component_name"].unique())
    n_comp = len(components)
    color_map = {c: PALETTE_4[i] for i, c in enumerate(components)}

    fig, axes = plt.subplots(1, len(test_types), figsize=(13, 6), sharey=True)
    fig.suptitle(
        "False Rate by Component — across Test Types",
        fontsize=14,
        fontweight="bold",
    )

    positions = np.arange(1, n_comp + 1)

    for ax, tt in zip(axes, test_types):
        sub = df[df["test_type"] == tt]
        data_per_comp = [
            sub.loc[sub["component_name"] == c, "false_rate"].dropna().values
            for c in components
        ]

        bp = ax.boxplot(
            data_per_comp,
            positions=positions,
            patch_artist=True,
            widths=0.55,
            medianprops=dict(color="white", linewidth=2.2),
            whiskerprops=dict(linewidth=1.3),
            capprops=dict(linewidth=1.3),
            flierprops=dict(marker="o", markersize=4.5, alpha=0.5, linestyle="none"),
        )
        for patch, comp in zip(bp["boxes"], components):
            patch.set_facecolor(color_map[comp])
            patch.set_alpha(0.82)
        for flier, comp in zip(bp["fliers"], components):
            flier.set_markerfacecolor(color_map[comp])

        ax.set_title(f"Test Type: {tt.upper()}", fontsize=11, fontweight="bold", pad=8)
        ax.set_xticks(positions)
        ax.set_xticklabels(components, rotation=22, ha="right", fontsize=9)
        ax.set_ylim(-0.05, 1.05)
        ax.yaxis.set_major_locator(MultipleLocator(0.2))
        ax.set_xlabel("Component", fontsize=10)

    axes[0].set_ylabel("False Rate", fontsize=11)

    legend_patches = [
        mpatches.Patch(facecolor=color_map[c], alpha=0.82, label=c) for c in components
    ]
    fig.legend(
        handles=legend_patches,
        title="Component",
        loc="lower center",
        ncol=n_comp,
        bbox_to_anchor=(0.5, -0.04),
        fontsize=9,
    )
    fig.tight_layout(rect=[0, 0.06, 1, 1])

    fig.savefig(
        os.path.join(OUTPUT_DIR, "plot2_falserate_by_component.png"),
        bbox_inches="tight",
    )
    plt.close(fig)
    print("✔  Plot 2 saved.")


# ══════════════════════════════════════════════════════════════════════════════
# Plot 3 — False Rate by Test Type × File Size  (grouped box plot, ordered)
# ══════════════════════════════════════════════════════════════════════════════
def plot3():
    # Keep only sizes present in the data, preserving canonical order
    present_sizes = [s for s in FILE_SIZE_ORDER if s in df["file_size"].unique()]
    n_sizes = len(present_sizes)
    color_map = {s: PALETTE_5[i] for i, s in enumerate(present_sizes)}

    fig, axes = plt.subplots(1, len(test_types), figsize=(14, 6), sharey=True)
    fig.suptitle(
        "False Rate by File-Size Bucket — across Test Types",
        fontsize=14,
        fontweight="bold",
    )

    positions = np.arange(1, n_sizes + 1)

    for ax, tt in zip(axes, test_types):
        sub = df[df["test_type"] == tt]
        data_per_size = [
            sub.loc[sub["file_size"] == s, "false_rate"].dropna().values
            for s in present_sizes
        ]

        bp = ax.boxplot(
            data_per_size,
            positions=positions,
            patch_artist=True,
            widths=0.55,
            medianprops=dict(color="white", linewidth=2.2),
            whiskerprops=dict(linewidth=1.3),
            capprops=dict(linewidth=1.3),
            flierprops=dict(marker="o", markersize=4.5, alpha=0.5, linestyle="none"),
        )
        for patch, sz in zip(bp["boxes"], present_sizes):
            patch.set_facecolor(color_map[sz])
            patch.set_alpha(0.82)
        for flier, sz in zip(bp["fliers"], present_sizes):
            flier.set_markerfacecolor(color_map[sz])

        # Annotate median values
        for pos, vals in zip(positions, data_per_size):
            if len(vals):
                med = np.median(vals)
                ax.text(
                    pos,
                    med + 0.04,
                    f"{med:.2f}",
                    ha="center",
                    va="bottom",
                    fontsize=7.5,
                    color="dimgrey",
                    fontweight="bold",
                )

        ax.set_title(f"Test Type: {tt.upper()}", fontsize=11, fontweight="bold", pad=8)
        ax.set_xticks(positions)
        pretty_labels = [s.replace("_", "–") for s in present_sizes]
        ax.set_xticklabels(pretty_labels, rotation=25, ha="right", fontsize=9)
        ax.set_ylim(-0.05, 1.05)
        ax.yaxis.set_major_locator(MultipleLocator(0.2))
        ax.set_xlabel("File Size (Nb Lines)", fontsize=10)

    axes[0].set_ylabel("False Rate", fontsize=11)

    legend_patches = [
        mpatches.Patch(facecolor=color_map[s], alpha=0.82, label=s.replace("_", "–"))
        for s in present_sizes
    ]
    fig.legend(
        handles=legend_patches,
        title="File Size",
        loc="lower center",
        ncol=n_sizes,
        bbox_to_anchor=(0.5, -0.05),
        fontsize=9,
    )
    fig.tight_layout(rect=[0, 0.07, 1, 1])

    fig.savefig(
        os.path.join(OUTPUT_DIR, "plot3_falserate_by_filesize.png"), bbox_inches="tight"
    )
    plt.close(fig)
    print("✔  Plot 3 saved.")


# ══════════════════════════════════════════════════════════════════════════════
# Plot 4 — Heatmap: mean false_rate  (component × file_size per test_type)
# ══════════════════════════════════════════════════════════════════════════════
def plot4():
    present_sizes = [s for s in FILE_SIZE_ORDER if s in df["file_size"].unique()]
    components = sorted(df["component_name"].unique())

    fig, axes = plt.subplots(
        1, len(test_types), figsize=(5 * len(test_types), 5), sharey=True
    )
    fig.suptitle(
        "Mean False Rate Heatmap\n(Component × File Size per Test Type)",
        fontsize=13,
        fontweight="bold",
    )

    for ax, tt in zip(axes, test_types):
        sub = df[df["test_type"] == tt]
        pivot = (
            sub.groupby(["component_name", "file_size"])["false_rate"]
            .mean()
            .unstack("file_size")
            .reindex(columns=present_sizes)
            .reindex(index=components)
        )
        sns.heatmap(
            pivot,
            ax=ax,
            cmap="YlOrRd",
            vmin=0,
            vmax=1,
            annot=True,
            fmt=".2f",
            annot_kws={"size": 9},
            linewidths=0.5,
            linecolor="white",
            cbar_kws={"label": "Mean False Rate", "shrink": 0.75},
        )
        ax.set_title(f"Test Type: {tt.upper()}", fontsize=11, fontweight="bold", pad=8)
        ax.set_xlabel("File Size (Nb Lines)", fontsize=10)
        ax.set_ylabel("Component" if ax is axes[0] else "", fontsize=10)
        ax.set_xticklabels(
            [s.replace("_", "–") for s in present_sizes],
            rotation=30,
            ha="right",
            fontsize=9,
        )

    fig.tight_layout()
    fig.savefig(
        os.path.join(OUTPUT_DIR, "plot4_heatmap_component_filesize.png"),
        bbox_inches="tight",
    )
    plt.close(fig)
    print("✔  Plot 4 saved.")


# ══════════════════════════════════════════════════════════════════════════════
# Plot 5 — Strip / Swarm plot: individual false_rate points coloured by
#           file_size, faceted by test_type
# ══════════════════════════════════════════════════════════════════════════════
def plot5():
    present_sizes = [s for s in FILE_SIZE_ORDER if s in df["file_size"].unique()]
    color_map = {s: PALETTE_5[i] for i, s in enumerate(present_sizes)}

    fig, axes = plt.subplots(1, len(test_types), figsize=(12, 6), sharey=True)
    fig.suptitle(
        "Individual False-Rate Observations\n"
        "Coloured by File-Size Bucket, per Test Type",
        fontsize=13,
        fontweight="bold",
    )

    components = sorted(df["component_name"].unique())
    x_pos = {c: i for i, c in enumerate(components)}

    for ax, tt in zip(axes, test_types):
        sub = df[df["test_type"] == tt].copy()
        sub["x"] = sub["component_name"].map(x_pos)

        for sz in present_sizes:
            mask = sub["file_size"] == sz
            jitter = np.random.uniform(-0.18, 0.18, mask.sum())
            ax.scatter(
                sub.loc[mask, "x"] + jitter,
                sub.loc[mask, "false_rate"],
                color=color_map[sz],
                alpha=0.65,
                s=35,
                edgecolors="none",
                label=sz.replace("_", "–"),
                zorder=3,
            )

        # component means
        for c, xi in x_pos.items():
            vals = sub.loc[sub["component_name"] == c, "false_rate"].dropna()
            if len(vals):
                ax.hlines(
                    vals.mean(),
                    xi - 0.35,
                    xi + 0.35,
                    colors="black",
                    linewidth=2,
                    zorder=4,
                )

        ax.set_xticks(list(x_pos.values()))
        ax.set_xticklabels(components, rotation=22, ha="right", fontsize=9)
        ax.set_ylim(-0.05, 1.05)
        ax.yaxis.set_major_locator(MultipleLocator(0.2))
        ax.set_title(f"Test Type: {tt.upper()}", fontsize=11, fontweight="bold", pad=8)
        ax.set_xlabel("Component", fontsize=10)

    axes[0].set_ylabel("False Rate", fontsize=11)

    # unified legend
    legend_patches = [
        mpatches.Patch(facecolor=color_map[s], alpha=0.8, label=s.replace("_", "–"))
        for s in present_sizes
    ]
    legend_patches.append(mpatches.Patch(facecolor="black", label="Component mean"))
    fig.legend(
        handles=legend_patches,
        title="File Size",
        loc="lower center",
        ncol=len(present_sizes) + 1,
        bbox_to_anchor=(0.5, -0.04),
        fontsize=9,
    )
    fig.tight_layout(rect=[0, 0.06, 1, 1])

    fig.savefig(
        os.path.join(OUTPUT_DIR, "plot5_stripplot_observations.png"),
        bbox_inches="tight",
    )
    plt.close(fig)
    print("✔  Plot 5 saved.")


# ── Run all ────────────────────────────────────────────────────────────────────
if __name__ == "__main__":
    print(f"Loaded {len(df)} rows from '{INPUT_CSV}'")
    print(f"Test types : {test_types}")
    print(f"Components : {sorted(df['component_name'].unique())}")
    print(f"File sizes : {sorted(df['file_size'].unique())}\n")

    plot1()
    plot2()
    plot3()
    plot4()
    plot5()

    print(f"\nAll plots saved to '{OUTPUT_DIR}/'")
