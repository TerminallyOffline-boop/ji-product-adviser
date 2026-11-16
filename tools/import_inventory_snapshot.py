"""Import the One Variant sheet as a bundled stock snapshot.

The serial-number sheet is intentionally ignored. Run with Python/openpyxl and
the workbook supplied by JI Telecom; the catalog stores product variants only.
"""
from __future__ import annotations

import difflib
import argparse
import hashlib
import json
import re
import statistics
import unicodedata
from pathlib import Path

import openpyxl

ROOT = Path(__file__).resolve().parents[1]
CATALOG = ROOT / "app/src/main/assets/ji_catalog_july_2026.json"
SPECS = ROOT / "app/src/main/assets/ji_official_specs_2026.json"
SNAPSHOT_DATE = "2026-11-16"


def normalize(text: str) -> str:
    text = unicodedata.normalize("NFKD", text).lower().replace("+", " plus ")
    text = re.sub(r"\b\d+\s*/\s*\d+\b", " ", text)
    text = re.sub(r"\b\d+\s*(?:gb|tb)\b", " ", text)
    text = re.sub(r"\b(?:live\s+demo\s+unit|wifi|wi-fi|with\s+keyboard|w/\s*keyboard|keyboard)\b", " ", text)
    text = re.sub(r"\([^)]*\)", " ", text)
    return re.sub(r"[^a-z0-9]+", " ", text).strip()


def capacities(text: str) -> tuple[int | None, int | None]:
    match = re.search(r"\b(\d+)\s*(?:\+\s*\d+\s*)?/\s*(\d+)\s*(?:gb)?\b", text, re.I)
    if not match:
        match = re.search(r"\b(\d+)\s*gb\s*/\s*(\d+)\s*gb\b", text, re.I)
    ram, storage = (int(match.group(1)), int(match.group(2))) if match else (None, None)
    tb = re.search(r"\b(\d+)\s*tb\b", text, re.I)
    if tb:
        storage = int(tb.group(1)) * 1024
    elif storage is None:
        found = re.search(r"\b(\d+)\s*gb\b", text, re.I)
        if found:
            storage = int(found.group(1))
    return ram, storage


def model_name(description: str, brand: str) -> str:
    model = re.sub(r"^" + re.escape(brand) + r"\s*", "", description, flags=re.I).strip()
    model = model.replace("Inifinix", "Infinix").replace("Real Me", "realme")
    if brand.lower() == "tecno mobile":
        model = re.sub(r"^Tecno\s+", "", model, flags=re.I)
    if "Live Demo Unit" in description:
        model += " (Live Demo Unit)"
    return model


def add_assignment(mapping: dict, sku: str, source_sku: str) -> None:
    if source_sku in mapping:
        mapping[sku] = dict(mapping[source_sku])
        mapping[sku]["skus"] = [sku]


def main(workbook_path: Path) -> None:
    catalog = json.loads(CATALOG.read_text(encoding="utf-8"))
    specs = json.loads(SPECS.read_text(encoding="utf-8"))
    old_products = catalog["products"]
    by_sku = {p["sku"]: p for p in old_products}
    assignment_by_sku = {sku: a for a in specs["assignments"] for sku in a["skus"]}

    # Carefully curated naming aliases used only to find the same model family.
    aliases = {
        "600": "JUL26-PHN-079", "smart 20 hd": "JUL26-PHN-085",
        "reno 16f 5g": "JUL26-PHN-107", "a6 4g": "JUL26-PHN-103",
        "a6s 5g": "JUL26-PHN-105", "a6x": "JUL26-PHN-098",
        "poco m8s 5g": "JUL26-PHN-194", "c100": "JUL26-PHN-123",
        "c100i": "JUL26-PHN-121", "c85 4g": "JUL26-PHN-118",
        "c85 5g": "JUL26-PHN-120", "note 80": "JUL26-PHN-115",
        "galaxy a17": "JUL26-PHN-141", "camon 50 ultra 5g": "JUL26-PHN-166",
        "pova 5g curve 2": "JUL26-PHN-167", "spark 50 5g": "JUL26-PHN-163",
        "spark 50": "JUL26-PHN-161", "spark go3": "JUL26-PHN-157",
        "y05e": "JUL26-PHN-171", "y04": "JUL26-PHN-171",
        "v60 lite 5g": "JUL26-PHN-178", "v80 lite 5g": "JUL26-PHN-178",
        "redmi note 17": "OCT26-PHN-008", "pad 2 pro": "JUL26-TAB-203",
        "aspire lite al14 45p r9ue": "OCT26-LAP-002",
        "aspire lite al14 47p r9cq": "OCT26-LAP-003",
        "nb a15 43p r19x": "JUL26-LAP-014",
        "aspire lite 14 al14 32p 34re": "JUL26-LAP-011",
        "laptop 15 fc0863au": "JUL26-LAP-018", "laptop 15 fc0930au": "JUL26-LAP-018",
        "omnibook 5 laptop ai 16 af1062tu": "JUL26-LAP-017",
        "omnibook 5 laptop ai 16 af1063tu": "JUL26-LAP-023",
        "galaxy tab a11 lte": "JUL26-TAB-151", "t10s": "JUL26-TAB-131",
        "pad neo tlpad002": "JUL26-TAB-130", "pad plus tlpad04": "JUL26-TAB-131",
        "pad mini": "JUL26-TAB-134", "redmi pad 2 9 7 wifi": "JUL26-TAB-197",
    }

    workbook = openpyxl.load_workbook(workbook_path, data_only=True, read_only=True)
    rows = []
    for values in workbook["One Variant"].iter_rows(values_only=True):
        division, brand, description, quantity, colors = (list(values) + [None] * 5)[:5]
        if division not in {"Phonekit", "Laptop", "Tablet"}:
            continue
        brand = str(brand).replace("Real Me", "realme").replace("Inifinix", "Infinix").replace("iTel", "itel")
        category = {"Phonekit": "SMARTPHONE", "Laptop": "LAPTOP", "Tablet": "TABLET"}[division]
        model = model_name(str(description), brand)
        if "Magbadge Freebies" in model:
            category = "ACCESSORIES"
        ram, storage = capacities(str(description))
        rows.append((brand, model, category, int(quantity), int(colors or 0), ram, storage, str(description)))
    if len(rows) != 172:
        raise ValueError(f"Expected 172 One Variant rows, found {len(rows)}")

    # Family aliases use exact product family labels; fallback matching also requires identical number tokens.
    family_source = {}
    for product in old_products:
        family_source[(product["brand"].lower(), product["category"], normalize(product["model"]))] = product
    used_existing: set[str] = set()
    imported = []
    new_assignments = []
    for brand, model, category, quantity, colors, ram, storage, description in rows:
        family = normalize(model)
        source_sku = next((sku for label, sku in aliases.items() if family.startswith(normalize(label))), None)
        candidates = []
        for product in old_products:
            if product["brand"].lower() != brand.lower() or product["category"] != category:
                continue
            candidate_family = normalize(product["model"])
            score = difflib.SequenceMatcher(None, family, candidate_family).ratio()
            if re.findall(r"\d+", family) == re.findall(r"\d+", candidate_family) and score >= .9:
                candidates.append((score, product))
        candidates.sort(key=lambda item: item[0], reverse=True)
        family_product = by_sku.get(source_sku) if source_sku else (candidates[0][1] if candidates else None)
        exact = None
        for score, product in candidates:
            if score < .96 or product["sku"] in used_existing:
                continue
            if product.get("ramGB") not in (None, ram) or product.get("storageGB") not in (None, storage):
                continue
            exact = product
            break
        if exact:
            sku = exact["sku"]
            used_existing.add(sku)
        else:
            key = f"{category}|{brand}|{description}|{ram}|{storage}"
            digest = hashlib.sha1(key.encode()).hexdigest()[:8].upper()
            prefix = {"SMARTPHONE": "PHN", "LAPTOP": "LAP", "TABLET": "TAB", "ACCESSORIES": "ACC"}[category]
            sku = f"INV26-{prefix}-{digest}"

        base_assignment = assignment_by_sku.get(family_product["sku"]) if family_product else None
        if base_assignment:
            assignment = dict(base_assignment)
            assignment["skus"] = [sku]
            assignment["ramGB"] = ram if ram is not None else assignment.get("ramGB")
            assignment["storageGB"] = storage if storage is not None else assignment.get("storageGB")
            new_assignments.append(assignment)

        if exact:
            price = float(exact.get("price", 0))
        elif family_product and family_product.get("price", 0):
            price = float(family_product["price"])
        else:
            pool = [p["price"] for p in old_products if p["brand"].lower() == brand.lower() and p["category"] == category and p.get("price", 0)]
            if not pool:
                pool = [p["price"] for p in old_products if p["category"] == category and p.get("price", 0)]
            price = round(statistics.median(pool) / 500) * 500 if pool else 0

        demo = "Live Demo Unit" in description
        status = "DISPLAY_UNIT" if demo else "UNAVAILABLE" if quantity <= 0 else "LIMITED" if quantity <= 2 else "AVAILABLE"
        note = (f"Inventory snapshot from {workbook_path.name}, One Variant sheet, dated {SNAPSHOT_DATE}; quantities include recorded adjustments. "
                f"Color variants: {colors}. The workbook contains no prices; catalog price is a generalized estimate, not a live quote.")
        if quantity < 0:
            note += " Negative adjusted net quantity is preserved as reported."
        if demo:
            note += " Display/demo item; excluded from purchase recommendations."
        item = {
            "sku": sku, "brand": brand, "model": model, "modelFamily": None,
            "variant": f"{ram} GB RAM / {storage} GB storage" if ram is not None and storage is not None else None,
            "category": category, "price": price, "promotionalPrice": None, "availabilityStatus": status,
            "inventorySnapshot": quantity, "ramGB": ram, "storageGB": storage, "notes": note,
            "sourceName": workbook_path.name, "lastUpdated": SNAPSHOT_DATE, "verificationStatus": "NEEDS_REVIEW"
        }
        if exact:
            for key in ("brand", "model", "modelFamily", "variant", "category", "price", "promotionalPrice"):
                if key in exact:
                    item[key] = exact[key]
            item["ramGB"] = ram if ram is not None else exact.get("ramGB")
            item["storageGB"] = storage if storage is not None else exact.get("storageGB")
            item["verificationStatus"] = base_assignment.get("verificationStatus", "NEEDS_REVIEW") if base_assignment else exact.get("verificationStatus", "NEEDS_REVIEW")
        imported.append(item)

    import_skus = {p["sku"] for p in imported}
    combined = []
    for product in old_products:
        if product["sku"] in import_skus:
            continue
        record = dict(product)
        record.update(inventorySnapshot=0, availabilityStatus="UNAVAILABLE", lastUpdated=SNAPSHOT_DATE)
        record["notes"] = (record.get("notes", "") + f" Not present in the complete inventory snapshot dated {SNAPSHOT_DATE}; treated as zero stock for recommendations.").strip()
        combined.append(record)
    combined.extend(imported)
    if len({p["sku"] for p in combined}) != len(combined):
        raise ValueError("Duplicate generated SKU; no assets written")

    retained_assignments = [a for a in specs["assignments"] if not any(sku in import_skus for sku in a["skus"])]
    catalog.update(catalogVersion="2026.11.16.1", sourceName="JI Telecom catalog with Inventory_One_Variant.xlsx stock snapshot",
                   sourceSheet=f"{workbook_path.name} / One Variant (variant counts; serial-number sheet excluded)", products=combined)
    specs.update(version="2026.11.16.1", verifiedDate=SNAPSHOT_DATE, assignments=retained_assignments + new_assignments)
    CATALOG.write_text(json.dumps(catalog, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    SPECS.write_text(json.dumps(specs, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    assigned = {sku for assignment in new_assignments for sku in assignment["skus"]}
    print(f"Imported {len(imported)} inventory variants; bundled catalog now {len(combined)} products.")
    print(f"{len(assigned)}/{len(imported)} variants linked to a hardware specification assignment.")
    print(f"Negative inventory entries: {[p['model'] for p in imported if (p['inventorySnapshot'] or 0) < 0]}")


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("workbook", type=Path, help="Path to the inventory workbook")
    main(parser.parse_args().workbook)
