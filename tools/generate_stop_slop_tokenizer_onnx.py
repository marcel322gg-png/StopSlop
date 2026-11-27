"""Rebuild the pinned XLM-R tokenizer graph used by StopSlop's E5 provider."""

import argparse
from pathlib import Path

import onnx
from onnx import helper
from onnxruntime_extensions._hf_cvt import HFTokenizerOnnxGraph
from transformers import AutoTokenizer


def main() -> None:
    parser = argparse.ArgumentParser()
    parser.add_argument("tokenizer_dir", type=Path,
                        help="Local immutable snapshot containing tokenizer.json and tokenizer_config.json")
    parser.add_argument("output", type=Path)
    args = parser.parse_args()

    tokenizer = AutoTokenizer.from_pretrained(
        str(args.tokenizer_dir), local_files_only=True, use_fast=True
    )
    graph = HFTokenizerOnnxGraph(tokenizer).pre_processing()
    model = helper.make_model(
        graph,
        opset_imports=[
            helper.make_operatorsetid("", 18),
            helper.make_operatorsetid("ai.onnx.contrib", 1),
        ],
        producer_name="StopSlop tokenizer preprocessor",
    )
    model.ir_version = 8
    onnx.checker.check_model(model)
    args.output.parent.mkdir(parents=True, exist_ok=True)
    onnx.save(model, args.output)


if __name__ == "__main__":
    main()
