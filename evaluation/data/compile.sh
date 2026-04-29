#!/bin/bash

SRC_DIR="./original_files"
OUT_DIR="./compiled_code"

# Create output directory if it doesn't exist
mkdir -p "$OUT_DIR"

for file in "$SRC_DIR"/*.c; do
    [ -e "$file" ] || continue
    filename=$(basename "$file" .c)

    gcc "$file" -o "$OUT_DIR/$filename"
done
