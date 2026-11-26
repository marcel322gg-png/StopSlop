# StopSlop - Global AI Model Catalogue

**Research snapshot:** 26 November 2026<br>
**Catalogue revision:** 1.1<br>
**Purpose:** broad research registry for future StopSlop on-device model packages<br>
**Status:** research inventory - **none of the entries below is approved as a StopSlop Stable installable package**

> This is intentionally a broad worldwide inventory, not a four-model recommendation list. It contains multilingual families, language-specialist encoders, compact/static embeddings, larger/high-quality candidates and text generators. An upstream model existing does not prove that it is mobile-ready, legally redistributable in a bundled package, accurate for StopSlop, or compatible with the current Morphe/TikTok runtime. Exact immutable revisions, full artifact sizes and licences must be rechecked before Stable publication.

## 1. How to read this catalogue

### Evidence labels

- `DECLARED` - language/task support appears in the upstream model card or maintainer documentation.
- `INDEPENDENT` - support or task has external benchmark evidence, but is not yet tested by StopSlop.
- `STOP SLOP TESTED` - the exact artifact and task passed the StopSlop dataset on a named device/runtime. No candidate in this research snapshot has this status.
- `UNKNOWN` - not reliably established yet.

### Install readiness

- `RESEARCH_ONLY` - candidate discovered; no StopSlop-compatible package proven.
- `CONVERSION_REQUIRED` - likely needs export, quantization, tokenizer adaptation or ARM64 rebuild.
- `RUNTIME_BLOCKED` - package/runtime integration is not proven in Morphe/TikTok.
- `BENCHMARK_PENDING` - exact task/language quality is not evaluated by StopSlop.
- `STABLE_INSTALLABLE` - reserved for signed, hashed, licence-checked and device-validated packages. **No entry currently has this status.**

### Important size terminology

Parameter count (e.g. 30M) is not download size. Model-card repository size is not necessarily the exact one-tap package size. Some models require a tokenizer, vocabulary, configuration, sentence-transformer modules, quantization tables or multiple shards. Runtime RAM can be several times larger than the downloaded weights. Every eventual Stable model card must show:

1. exact total download bytes for all required files;
2. exact installed bytes after extraction/promotion;
3. peak RAM and cold-start only when measured;
4. runtime/ABI requirements;
5. the hardware-fit score separately from language quality.

## 2. Broad multilingual embedding candidates

These are the first candidates to investigate for users who select many languages or `🌍 Any language`.

| Model ID | Publicly documented scope | Size/asset evidence currently known | Licence | StopSlop assessment / missing proof |
|---|---|---|---|---|
| `ibm-granite/granite-embedding-97m-multilingual-r2` | General embeddings for 200+ pretrained languages; enhanced retrieval/cross-lingual training for 52 languages, including `ru`, `uk`, `en`, `ar`, `zh`, `ja`, `ko`, `hi`, `tr`, `vi` | 97M parameters. One full safetensors file was reported at ~195 MB plus tokenizer ~25.3 MB. A quantized ONNX artifact around 98.2 MB is labelled AVX2/x86; **not ARM64 proof**. | Apache-2.0 | High-priority compact multilingual candidate. Needs ARM64 mobile conversion, exact package size, runtime and StopSlop benchmark. |
| `ibm-granite/granite-embedding-311m-multilingual-r2` | Same broad multilingual family; enhanced support for 52 languages | 311M parameters; full package size/runtime profile to verify | Apache-2.0 | Quality-3/high-resource comparison; not a Compact default. |
| `ibm-granite/granite-embedding-107m-multilingual` | 12-language multilingual embedding candidate | 107M parameters; exact deployable package size to verify | Apache-2.0 | Potential middle option; check exact language set and mobile export. |
| `intfloat/multilingual-e5-small` | About 99 language tags in MTEB listings; 118M parameters | FP model file reported around 470 MB. One quantized ONNX file around 118 MB is AVX512-VNNI-specific, not an ARM64 mobile artifact. Tokenizer files add more. | MIT | Broad coverage candidate; needs ARM64 export/quantization and complete package measurement. |
| `intfloat/multilingual-e5-base` | Multilingual E5; broader semantic retrieval candidate | 278M parameters class; exact package sizes/variants to verify | MIT | Higher-resource quality candidate. |
| `intfloat/multilingual-e5-large` | Multilingual E5; high-resource variant | Large model; exact parameters/package variant to verify | MIT | Highest-resource comparison only; likely unsuitable for low-end phones. |
| `sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2` | 53 languages in MTEB/Model Card metadata | 117.7M parameters; ~471 MB model file plus tokenizer JSON (~9 MB) and SentencePiece (~5 MB) in the inspected repository snapshot | Apache-2.0 | Strong baseline family, but needs quantized ARM64 package. |
| `sentence-transformers/paraphrase-multilingual-mpnet-base-v2` | 53 languages | 278M parameters; original model has high memory footprint; exact Android package size to verify | Apache-2.0 | Higher quality/resource tier candidate. |
| `sentence-transformers/distiluse-base-multilingual-cased-v2` | Broad multilingual sentence-transformer family; commonly listed for 50+ languages | Exact immutable artifact/file sizes to verify | Apache-2.0 (recheck exact revision) | Candidate for balanced mobile comparison; verify conversion and exact language list. |
| `sentence-transformers/LaBSE` | 109 languages | Large encoder; exact mobile quantized package size to verify | Apache-2.0 | Broad coverage/high-resource reference, not a low-end promise. |
| `minishlab/potion-multilingual-128M` | 101 languages | 128M static token embeddings; MTEB lists ~489 MB memory for its benchmark setup (not a download-size claim) | MIT | Very fast static-embedding architecture; compare quality against contextual models and verify actual package size. |
| `Horizon-Labs/multilingual-embedding-small` | About 90 languages reported | 141M parameters; quantized ONNX artifact reported around 269 MB | Apache-2.0 | Broad candidate; verify ARM64 performance, tokenizer and exact package. |
| `litert-community/embeddinggemma-2-text-270m-litert-lm` (Google EmbeddingGemma 2 Text 270M) | Text embeddings for 100+ languages | 270M parameters; published text-only `.litertlm` file is 165 MB; hardware-specific variants range higher; pin exact revision/bytes | Apache-2.0 for EmbeddingGemma 2 | Mobile-oriented candidate; must prove LiteRT-LM and Morphe host integration. Do not confuse with the 440M/740M multimodal variants. |
| `BAAI/bge-m3` | Broad multilingual retrieval, 100+ languages | 568M parameters; exact mobile quantized package size/runtime to verify | MIT (recheck exact revision) | High-capability reference; likely not a compact option. |
| `facebook/SONAR` | 200+ languages in documented language coverage | Model family/size varies; exact text-only artifact/runtime must be pinned | MIT | Very broad language research candidate; mobile footprint and StopSlop sentence-matching quality unknown. |
| `minishlab/potion-retrieval-32M` | English | 32.3M parameters; MTEB lists ~123 MB memory for benchmarked use (not exact file size) | MIT | Compact English static-embedding candidate; not multilingual. |

## 3. Compact English-specialist candidates

These can serve users who select English only. They must not be advertised as multilingual.

| Model ID | Scope | Public size/parameter evidence | Licence | StopSlop assessment |
|---|---|---|---|---|
| `sentence-transformers/all-MiniLM-L6-v2` | English | 22.7M parameters; exact quantized mobile file set to verify | Apache-2.0 (verify exact revision) | Strong tiny baseline candidate for English captions. |
| `BAAI/bge-small-en-v1.5` | English | 33.4M parameters; mobile artifact size to verify | MIT | Compact English embedding candidate. |
| `jinaai/jina-embeddings-v2-small-en` | English | 33M parameters; exact mobile package size to verify | Apache-2.0 | Compact English candidate; inspect model architecture/runtime support. |
| `intfloat/e5-small-v2` | English | 33M parameters class; exact quantized mobile package size to verify | MIT | Compact retrieval candidate. |
| `minishlab/blink-8M` | English | 7.7M parameters; MTEB lists ~29 MB memory for benchmarked use (not file size) | MIT | Very small static embedding; likely lowest-resource test candidate. |
| `minishlab/M2V_base_output` | English | 7.6M parameters; MTEB lists ~29 MB memory for benchmarked use (not file size) | MIT | Tiny static embeddings; compare semantic quality and exact artifact size. |

## 4. Russian / Russian-English specialists

| Model ID | Scope | Public size/parameter evidence | Licence | StopSlop assessment |
|---|---|---|---|---|
| `cointegrated/rubert-tiny` | Russian and English | 12M parameters; model card reports ~45 MB; sentence representation support is described by author | MIT | Extremely compact candidate; quality for modern cross-lingual semantic moderation needs direct testing. |
| `cointegrated/rubert-tiny2` | Russian-focused | ~29M parameters class; exact quantized file set to verify | MIT | Stronger Russian NLU/sentence embedding candidate; no general multilingual claim. |
| `sergeyzh/rubert-tiny-sts-v2` | Russian/English sentence similarity | ~29M parameters class; exact package bytes to verify | MIT | Directly relevant STS candidate; benchmark against Russian slang, misspellings and short captions. |
| `sergeyzh/rubert-mini-frida` | Russian sentence embeddings | 32.3M parameters; MTEB lists ~123 MB memory in benchmarked setup, not exact file size | MIT | Russian specialist; verify tokenizer and Android conversion. |
| `deepvk/USER2-small` | Russian semantic embeddings | 34.4M parameters; inspected files include ~138 MB safetensors plus ~4.75 MB tokenizer JSON | Apache-2.0 | Candidate for RU-specialist accuracy; package size is larger than its parameter count suggests. |
| `deepvk/USER2-base` | Russian semantic embeddings | Larger sibling; exact immutable revision and full artifact size to verify | Apache-2.0 | Quality-tier comparison for higher-resource RU devices. |
| `sergeyzh/BERTA` | Russian embeddings | 128M parameters; MTEB records high memory use in reference benchmark | MIT | Larger RU specialist/reference. |

## 5. Regional and language-specialist candidates

The list is intentionally broader than the languages currently used in StopSlop tests. Every entry remains a research candidate until exact artifacts, licences and language evidence are verified.

| Language/region | Model ID | Public evidence | Licence / size state | StopSlop assessment |
|---|---|---|---|---|
| Japanese | `cl-nagoya/ruri-v3-30m` | Japanese embedding model; ~37M parameter class | Apache-2.0; one inspected file set contains ~147 MB weights plus tokenizer assets (~6.7 MB JSON + ~1.8 MB SentencePiece) | Demonstrates why parameter count must not be used as download size. |
| Japanese | `cl-nagoya/ruri-v3-70m` | Japanese embedding model, 70M class | Apache-2.0; full mobile package size to verify | Higher-capacity Japanese specialist. |
| Korean | `dragonkue/multilingual-e5-small-ko` | Korean/English semantic embedding | Apache-2.0; MTEB records ~449 MB memory in reference setup; package size to verify | Korean-specialist E5 variant; large for a 118M model once runtime overhead included. |
| Korean | `dragonkue/multilingual-e5-small-ko-v2` | Korean/English embedding variant | Apache-2.0; package size to verify | Compare to v1 on Korean slang and mixed Korean/English. |
| Korean | `NGA-KR/ko-embed-nli` | Korean embedding task | MIT in MTEB listing; exact package size to verify | Research candidate. |
| Korean | `snunlp/KR-SBERT-V40K-klueNLI-augSTS` | Korean sentence similarity | Exact licence, model bytes and mobile format not verified | Research-only until metadata confirmed. |
| Korean | `BM-K/KoSimCSE-roberta-multitask` | Korean sentence embedding/similarity | Repository includes large weights (~442 MB in inspected listing); licence needs recheck | High-resource candidate, not compact by assumption. |
| Chinese | `moka-ai/m3e-small` | Chinese/English embedding candidate | Exact licence/package files must be rechecked | Research-only until immutable card metadata is confirmed. |
| Chinese | `shibing624/text2vec-base-chinese` | Chinese sentence embeddings | Apache-2.0; inspected safetensors ~409 MB | Useful benchmark reference, not a low-end option. |
| Arabic | `ALJIACHI/Mizan-Embeddings-0.1B` | Arabic semantic embedding; 149M class | Apache-2.0; exact artifact package size to verify | Arabic specialist; measure dialect coverage separately. |
| Vietnamese | `bkai-foundation-models/vietnamese-bi-encoder` | Vietnamese bi-encoder candidate | Licence, exact size and mobile artifacts need verification | Research-only. |
| Vietnamese | `AITeamVN/Vietnamese_Embedding_v2` | Vietnamese embedding candidate | Larger (~0.6B class reported); exact licence/package needs verification | High-resource comparison; not a Compact model. |
| Nordic | `NbAiLab/nb-bert-base` | Norwegian Bokmål/Nynorsk encoders | CC-BY-4.0; 177.9M parameters | Not a sentence embedder by default; requires task-specific pooling/fine-tuning. |
| Nordic | `NbAiLab/nb-bert-large` | Norwegian Bokmål/Nynorsk | CC-BY-4.0; 355.1M parameters | Higher-resource encoder; not a compact mobile model. |
| Hindi / Indian languages | `google/muril-base-cased` | Multilingual Indian-language encoder family | Exact licence/artifact and language-task coverage to recheck | Encoder candidate; not automatically a sentence-embedding model. |
| Hindi / Indian languages | `ai4bharat/indic-bert-v2-MLM-only` | Indian-language pretrained encoder family | Exact revision/licence/package and sentence-similarity suitability to verify | Research-only; may need fine-tuning/pooling. |
| Bengali | `csebuetnlp/banglabert` | Bengali language encoder | Licence/package and sentence-embedding suitability to verify | Research-only; do not list as a ready semantic model. |
| Turkish | `dbmdz/bert-base-turkish-cased` | Turkish encoder | Licence and sentence-embedding conversion need verification | Research-only; needs sentence-level fine-tuning/pooling. |
| Arabic | `CAMeL-Lab/bert-base-arabic-camelbert-mix` | Arabic encoder covering multiple dialect/register sources | Licence and sentence-embedding suitability to verify | Research-only; not a ready embedding package. |

## 6. Generative models - separate catalogue task

A generator is needed only for producing structured aliases, translations and spelling alternatives when a user creates/edits a topic. It is not the same as an embedding model. A generator is not run for every feed item.

| Model ID | Public language/task evidence | Size/resource state | Licence | StopSlop assessment |
|---|---|---|---|---|
| `HuggingFaceTB/SmolLM2-135M-Instruct` | Small instruction generator; English-focused | 135M parameters; quantized mobile package size/runtime to verify | Apache-2.0 | Tiny English generator experiment, not a worldwide multilingual option. |
| `Qwen/Qwen3-0.6B` | Multilingual generative model family; broad language claims | 0.6B; exact mobile quantized package and RAM to verify | Apache-2.0 (verify exact revision) | Potential compact multilingual generator, but materially heavier than embeddings. Test RU/UK/EN output directly. |
| `Qwen/Qwen2.5-0.5B-Instruct` | Model card reports 29+ languages | 0.5B; exact mobile package to verify | Apache-2.0 (verify exact revision) | Generator candidate; evaluate hallucinations and structured output. |
| `google/gemma-3-270m-it` | Compact instruction generator | 270M; exact quantized runtime bundle to verify | Gemma terms, not Apache-2.0 | Candidate; do not infer language coverage without tests. |
| `meta-llama/Llama-3.2-1B-Instruct` | Eight official languages listed for the 1B family | 1B; large memory/quantized package | Llama Community License | Higher-resource reference, not a compact default. |

## 7. Initial language coverage matrix

The app must not hard-code this list. It is an initial research matrix showing that language coverage comes from a union of multilingual and specialist model metadata. The final UI is generated from the signed catalogue manifest.

### Strong multilingual starting coverage

IBM Granite Embedding 97M R2 documents **enhanced** retrieval support for these 52 tags (general pretraining coverage is separately claimed for 200+ languages):

` sq, ar, az, bn, bg, ca, zh, hr, cs, da, nl, en, et, fi, fr, ka, de, el, he, hi, hu, is, id, it, ja, kk, km, ko, lv, lt, ms, mr, no, fa, pl, pt, ro, ru, sr, sk, sl, es, sw, sv, tl, te, th, tr, uk, ur, uz, vi `

Other multilingual families add broad language lists: multilingual E5 is listed around 99 languages in MTEB; LaBSE 109; Potion multilingual 101; MiniLM-L12 53. These are model-family claims, not identical quality across languages.

### Specialist search tracks

Continue discovery for:
- Slavic: Russian, Ukrainian, Polish, Czech, Slovak, Bulgarian, Serbian, Croatian, Slovenian, Macedonian;
- West/North European: English, German, French, Spanish, Portuguese, Italian, Dutch, Swedish, Norwegian, Danish, Finnish, Icelandic;
- South/East Asian: Chinese, Japanese, Korean, Hindi, Bengali, Marathi, Tamil, Telugu, Gujarati, Kannada, Malayalam, Punjabi, Urdu, Thai, Vietnamese, Indonesian, Malay, Khmer, Burmese, Filipino;
- Middle East/Central Asia: Arabic (including dialect evidence), Persian, Hebrew, Turkish, Azerbaijani, Kazakh, Uzbek, Georgian;
- African languages and low-resource scripts: Swahili and any credible candidates discovered through the model-hub index.

If no small specialist exists, a multilingual model is a valid candidate. Never invent a one-language model for catalogue completeness.

## 8. Quality levels and hardware fit are different

### Quality preference 1-3

- **1 Compact:** smallest tested model meeting a minimum per-language StopSlop threshold.
- **2 Balanced:** balanced size, measured quality and latency.
- **3 Highest verified quality:** best measured task/language results that remain within the device's safe budget.

All evaluations must be task-specific. An embedding model uses retrieval/similarity precision, recall, F1, ROC/PR, false-positive/false-negative and ranking results as suitable. A generator uses exact-structure validity, direct-alias precision, hallucination rate, language correctness and user-acceptance rate. Do not give one universal `accuracy 92%` badge.

### Hardware compatibility 0-100%

Hard gates first: Android API, ABI, runtime backend, format/operator support, licence, storage and memory. If a hard gate fails, show `Not compatible`, not a percentage.

For compatible models, a proposed transparent index is:
- peak-RAM headroom: 30%;
- inference latency vs target budget: 30%;
- cold-start time: 15%;
- thermal/stability: 15%;
- free storage and update-stage margin: 10%.

`Hardware fit 88%` is a device-fit score, not language quality or chance of working. It must say `Verified on this device` or `Estimated from same SoC/class`. If there is no measurement basis, show `Not measured`.

## 9. App security and delivery contract

- Keep model files outside APK/MPP and Git. The user selects and downloads only the chosen model package.
- Verify the actual TikTok host `Context` and package before relying on `filesDir`/`cacheDir`.
- Staging downloads belong under `cacheDir/stopSlop/downloads`; installed files under `filesDir/stopSlop/models/<id>/<version>`.
- No broad storage permission, SAF, shared Downloads or user file picker for normal one-tap install.
- Check the existing host `INTERNET` permission; do not change merged manifest blindly.
- Stable catalogue is a small signed JSON manifest hosted at a project-controlled HTTPS origin. It contains fixed immutable URLs, exact byte sizes, SHA-256, version, language evidence, license, runtime and ABI.
- Pin the catalogue verification public key in the shipped app. The chosen verifier must work on the actual minSdk or be packaged at build time. No executable code may be fetched.
- Download model data and tokenizer/config only. Reject DEX/JAR/`.so`, scripts, Python pickle, remote-code models, unknown operators, path traversal and oversized archive bombs.
- Update by staging a new version, verifying it, testing a model load, atomically switching and preserving rollback until success. Stable only; no beta/nightly for default users.
- On model delete, unload, remove weights/cache/registry entries and leave user topics/preferences intact. Mechanical mode remains.
- Inference is offline. No captions, videos, audio, topics, embeddings, creator/account details or moderation results leave device.

## 10. Research sources

Use these as starting points and recheck immutable revisions before publication:

- MTEB available text models: https://docs.mteb.org/overview/available_models/text/
- IBM Granite 97M multilingual R2: https://huggingface.co/ibm-granite/granite-embedding-97m-multilingual-r2
- IBM Granite 311M multilingual R2: https://huggingface.co/ibm-granite/granite-embedding-311m-multilingual-r2
- ONNX Community Granite export: https://huggingface.co/onnx-community/granite-embedding-97m-multilingual-r2-ONNX
- Multilingual E5 small: https://huggingface.co/intfloat/multilingual-e5-small
- Multilingual MiniLM-L12: https://huggingface.co/sentence-transformers/paraphrase-multilingual-MiniLM-L12-v2
- LaBSE: https://huggingface.co/sentence-transformers/LaBSE
- Potion multilingual: https://huggingface.co/minishlab/potion-multilingual-128M
- EmbeddingGemma 2 text-only LiteRT package (165 MB): https://huggingface.co/litert-community/embeddinggemma-2-text-270m-litert-lm
- EmbeddingGemma 2: https://ai.google.dev/gemma/docs/embeddinggemma
- Google AI Edge Semantic Retriever: https://developers.google.com/edge/mediapipe/solutions/retrieval/semantic_retriever
- RuBERT-tiny: https://huggingface.co/cointegrated/rubert-tiny
- RuBERT-tiny2: https://huggingface.co/cointegrated/rubert-tiny2
- RuBERT tiny STS v2: https://huggingface.co/sergeyzh/rubert-tiny-sts-v2
- USER2-small: https://huggingface.co/deepvk/USER2-small
- Ruri v3 30M: https://huggingface.co/cl-nagoya/ruri-v3-30m
- Ruri v3 70M: https://huggingface.co/cl-nagoya/ruri-v3-70m
- Mizan Arabic: https://huggingface.co/ALJIACHI/Mizan-Embeddings-0.1B
- SmolLM2: https://huggingface.co/HuggingFaceTB/SmolLM2-135M-Instruct
- Qwen3 0.6B: https://huggingface.co/Qwen/Qwen3-0.6B
- Qwen2.5 0.5B Instruct: https://huggingface.co/Qwen/Qwen2.5-0.5B-Instruct
- Android app-specific storage: https://developer.android.com/training/data-storage/app-specific
- LiteRT Android API/minimum SDK support: https://developers.google.com/edge/litert/android
- LiteRT-LM Android integration: https://developers.google.com/edge/litert-lm/android

## 11. Additional global candidates checked for revision 1.1

These model cards expand the discovery set across multilingual retrieval, regional encoders and text generation. They are candidate records, not installation recommendations. A source URL pointing to a model's `main` branch is mutable; **the exact model-repository commit is still unpinned for the entries below unless called out explicitly**. Every package remains `RESEARCH_ONLY` until a full immutable file manifest, licence review, ARM64/runtime proof and StopSlop evaluation are recorded. `UNKNOWN` means the relevant artifact detail was not verified in this pass.

| Model ID | Task and language evidence | Model / package / licence evidence | Mobile readiness and status |
|---|---|---|---|
| [`Qwen/Qwen3-Embedding-0.6B`](https://huggingface.co/Qwen/Qwen3-Embedding-0.6B) | Embedding; 0.6B parameters; publisher declares 100+ languages and multilingual/cross-lingual retrieval. Exact language tags: UNKNOWN. | Apache-2.0; full tokenizer/config/weight byte set and total package: UNKNOWN. | No verified Android ARM64 package for this repo revision; conversion/runtime/ABI/API UNKNOWN. `CONVERSION_REQUIRED`. |
| [`Alibaba-NLP/gte-multilingual-base`](https://huggingface.co/Alibaba-NLP/gte-multilingual-base) | Embedding; 305M parameters; card declares 75 languages. | Apache-2.0; exact pinned weight/tokenizer/config package bytes: UNKNOWN. | Card examples use `trust_remote_code` on older Transformers versions; ARM64 runtime/operator proof absent. `CONVERSION_REQUIRED`. |
| [`jinaai/jina-embeddings-v3`](https://huggingface.co/jinaai/jina-embeddings-v3) | Embedding; card says 94 foundation languages and 30 languages with focused tuning, including `ru` and `uk`. | CC-BY-NC-4.0; exact full file-set size: UNKNOWN. Card exposes ONNX as a format tag, which does not establish an Android ARM64 package. | Need immutable model-weight revision, static runtime/operator and ARM64 tests. `CONVERSION_REQUIRED`. |
| [`Snowflake/snowflake-arctic-embed-m-v1.5`](https://huggingface.co/Snowflake/snowflake-arctic-embed-m-v1.5) | Embedding/retrieval; 109M parameters. Specific supported language set: UNKNOWN. | Apache-2.0; card reports MTEB Retrieval NDCG@10 55.14. Full package bytes: UNKNOWN. | ONNX/GGUF tags are not Android validation; ARM64/runtime/API UNKNOWN. `CONVERSION_REQUIRED`. |
| [`nomic-ai/nomic-embed-text-v1.5`](https://huggingface.co/nomic-ai/nomic-embed-text-v1.5) | Embedding; English-oriented model metadata. Exact language declaration to pin: UNKNOWN. | Licence and full tokenizer/config/weight bytes: recheck at immutable revision before redistribution. | Custom-code/model-loading requirements and mobile ARM64 support need review. `RESEARCH_ONLY`. |
| [`moka-ai/m3e-small`](https://huggingface.co/moka-ai/m3e-small) | Embedding; card describes Chinese and English; 24M parameters. | Card notes non-commercial dataset restrictions. The inspected repository lists a ~95.8 MB `pytorch_model.bin` alongside tokenizer/config files; do not load this pickle-based artifact. Exact safe converted package: UNKNOWN. | Requires license review and a safe, static conversion. `RUNTIME_BLOCKED`. |
| [`KBLab/sentence-bert-swedish-cased`](https://huggingface.co/KBLab/sentence-bert-swedish-cased) | Sentence embedding; card describes Swedish and English; versions v1.0-v2.0 are named in the card. | Apache-2.0; full package bytes at a pinned v2.0 commit: UNKNOWN. | No verified Android ARM64 format/runtime. Pin the exact v2.0 snapshot before testing. `CONVERSION_REQUIRED`. |
| [`jhgan/ko-sroberta-multitask-mrl`](https://huggingface.co/jhgan/ko-sroberta-multitask-mrl) | Korean sentence embedding/retrieval candidate. | A related repository discussion by the owner states CC-BY-SA-4.0; the exact MRL artifact's licence inheritance, files and bytes: UNKNOWN. | Licence inheritance and redistribution terms need review; no Android ARM64 proof. `RESEARCH_ONLY`. |
| [`google/gemma-3-1b-it`](https://huggingface.co/google/gemma-3-1b-it) | Text generator; 1B class; Google describes Gemma 3 as supporting 140+ languages. This is declared coverage, not StopSlop-tested alias quality. | Gemma Terms of Use apply. A 584 MB int4 LiteRT-LM export is listed by LiteRT Community; that conversion is not an upstream immutable package pin. | LiteRT-LM Android runtime integration and host JNI delivery are unverified; extension minSdk is 23 while current LiteRT-LM requires API 24. `RUNTIME_BLOCKED`. |
| [`CohereLabs/aya-expanse-8b`](https://huggingface.co/CohereLabs/aya-expanse-8b) | Text generator; 8B; card declares 23 languages including Arabic, English, Russian and Ukrainian. | CC-BY-NC-4.0 plus Cohere Labs Acceptable Use Policy; gated access; package size/revision: UNKNOWN. | Resource-heavy research comparator, not an A25/A12 recommendation; no mobile runtime proof. `RUNTIME_BLOCKED`. |
| [`HuggingFaceTB/SmolLM3-3B`](https://huggingface.co/HuggingFaceTB/SmolLM3-3B) | Text generator; 3B; card declares six native languages: English, French, Spanish, German, Italian and Portuguese. | Licence and full artifact bytes must be pinned and rechecked before redistribution. | Card mentions ONNX/local desktop runtimes, not Android extension ARM64; resource fit UNKNOWN. `CONVERSION_REQUIRED`. |
| [`SeaLLMs/SeaLLMs-v3-1.5B`](https://huggingface.co/SeaLLMs/SeaLLMs-v3-1.5B) | Base text generator; card's model badge says 9 languages while its narrative lists a larger Southeast Asian set (including `my`, `km`, `lo`, `ta`, and Javanese). This conflict must be resolved before those languages are advertised. | Custom `seallms` licence; exact terms, immutable revision and full package bytes: UNKNOWN. | Base model is not instruction-tuned; chat variant and Android export require separate verification. `RESEARCH_ONLY`. |
| [`facebook/nllb-200-distilled-600M`](https://huggingface.co/facebook/nllb-200-distilled-600M) | Translation model, not an embedding or general alias generator; card indicates 196 languages. | CC-BY-NC-4.0; research-only and explicitly not released for production deployment. Inspected tokenizer JSON alone is 17.3 MB; total package UNKNOWN. | Large conversion/runtime and licence barriers; not a StopSlop Stable candidate. `RUNTIME_BLOCKED`. |

The current discovery tables contain **66 model references (65 distinct IDs)**: 53 IDs in the previous inventory plus 12 new distinct IDs in this expansion. Language claims span multilingual and regional sources. The prior explicit 52-tag list comes from one model's enhanced retrieval coverage, not from the whole catalogue. The additional Southeast Asian language claims above are not included in that list until the SeaLLMs metadata conflict is resolved. Candidate count is not a quality score or installability claim.

### Required record fields before any candidate advances

For each model intended for comparison, capture the immutable upstream commit (not `main`), every required file's path/bytes/SHA-256, complete package download and installed sizes, tokenizer/config/runtime companions, SPDX/custom licence evidence, BCP-47 tags with declared/tested distinction, conversion/export provenance, format/operators, Android API floor, native ABI, and an explicit readiness state. The rows above intentionally leave unknowns visible rather than filling them from parameter counts or unrelated converted copies.

### Sources rechecked for this expansion

- GTE multilingual base: https://huggingface.co/Alibaba-NLP/gte-multilingual-base
- Jina embeddings v3: https://huggingface.co/jinaai/jina-embeddings-v3
- Qwen3 embeddings: https://huggingface.co/Qwen/Qwen3-Embedding-0.6B
- Snowflake Arctic Embed M v1.5: https://huggingface.co/Snowflake/snowflake-arctic-embed-m-v1.5
- M3E small: https://huggingface.co/moka-ai/m3e-small
- Swedish sentence embeddings: https://huggingface.co/KBLab/sentence-bert-swedish-cased
- Korean sentence embeddings and owner licence clarification: https://huggingface.co/jhgan/ko-sroberta-multitask-mrl and https://huggingface.co/jhgan/ko-sroberta-multitask/discussions/13
- Gemma 3 1B: https://huggingface.co/google/gemma-3-1b-it and https://huggingface.co/litert-community/Gemma3-1B-IT/tree/main
- Aya Expanse: https://huggingface.co/CohereLabs/aya-expanse-8b
- SmolLM3: https://huggingface.co/HuggingFaceTB/SmolLM3-3B
- SeaLLMs v3: https://huggingface.co/SeaLLMs/SeaLLMs-v3-1.5B
- NLLB-200: https://huggingface.co/facebook/nllb-200-distilled-600M

---

**Research caveat:** the entries above establish a broad candidate landscape, not a list of immediately installable phone models. The first Stable releases may be few, then grow as the project converts, signs, tests and validates more candidates. This avoids promising 40 models that cannot safely run on the user's phone.
