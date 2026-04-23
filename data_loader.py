import json
import os

DATA_FILE = os.path.join(os.path.dirname(__file__), "data.json")

_papers = None


def load_papers():
    global _papers
    if _papers is None:
        with open(DATA_FILE, "r", encoding="utf-8") as f:
            data = json.load(f)
        _papers = data.get("papers", [])
    return _papers


def get_classes():
    papers = load_papers()
    return sorted(set(p["class"] for p in papers), key=lambda x: int(x))


def get_subjects(cls):
    papers = load_papers()
    return sorted(set(p["subject"] for p in papers if p["class"] == cls))


def get_years(cls, subject):
    papers = load_papers()
    return sorted(
        set(p["year"] for p in papers if p["class"] == cls and p["subject"] == subject),
        reverse=True,
    )


def find_paper(cls, subject, year):
    papers = load_papers()
    for p in papers:
        if (
            p["class"] == str(cls)
            and p["subject"].lower() == subject.lower()
            and p["year"] == str(year)
        ):
            return p
    return None
