"""
Help system router for online documentation and contextual help
"""

from fastapi import APIRouter, Depends, HTTPException, Query
from sqlalchemy.ext.asyncio import AsyncSession
from backend.database import get_db
from backend.routers.auth import get_current_user
from backend.models.user import User
from typing import Optional, List, Dict, Any
import json
import os
from pathlib import Path

router = APIRouter()

# Help content directory
HELP_CONTENT_DIR = Path(__file__).parent.parent / "data" / "help"


def load_help_content(filename: str) -> Dict[str, Any]:
    """Load help content from JSON file"""
    file_path = HELP_CONTENT_DIR / filename
    if not file_path.exists():
        return {}
    
    try:
        with open(file_path, 'r', encoding='utf-8') as f:
            return json.load(f)
    except Exception as e:
        return {"error": str(e)}


@router.get("/articles")
async def get_articles(
    category: Optional[str] = Query(None),
    role: Optional[str] = Query(None),
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get help articles with optional filtering"""
    workflows = load_help_content("workflows.json")
    concepts = load_help_content("concepts.json")
    
    articles = []
    
    # Add workflows
    if "workflows" in workflows:
        for workflow in workflows["workflows"]:
            if role and workflow.get("role") != role:
                continue
            if search and search.lower() not in workflow.get("title", "").lower() and search.lower() not in workflow.get("content", "").lower():
                continue
            articles.append({
                **workflow,
                "type": "workflow",
                "category": "workflows"
            })
    
    # Add concepts
    if "concepts" in concepts:
        for concept in concepts["concepts"]:
            if category and concept.get("category") != category:
                continue
            if search and search.lower() not in concept.get("title", "").lower() and search.lower() not in concept.get("content", "").lower():
                continue
            articles.append({
                **concept,
                "type": "concept",
                "category": concept.get("category", "concepts")
            })
    
    return {"articles": articles, "total": len(articles)}


@router.get("/articles/{article_id}")
async def get_article(
    article_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific help article"""
    workflows = load_help_content("workflows.json")
    concepts = load_help_content("concepts.json")
    
    # Search in workflows
    if "workflows" in workflows:
        for workflow in workflows["workflows"]:
            if workflow.get("id") == article_id:
                return {**workflow, "type": "workflow"}
    
    # Search in concepts
    if "concepts" in concepts:
        for concept in concepts["concepts"]:
            if concept.get("id") == article_id:
                return {**concept, "type": "concept"}
    
    raise HTTPException(status_code=404, detail="Article not found")


@router.get("/terminology")
async def get_terminology(
    search: Optional[str] = Query(None),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get terminology dictionary"""
    terms_data = load_help_content("terminology.json")
    
    if "terms" not in terms_data:
        return {"terms": [], "total": 0}
    
    terms = terms_data["terms"]
    
    if search:
        search_lower = search.lower()
        terms = [
            term for term in terms
            if search_lower in term.get("term", "").lower() or
               search_lower in term.get("novice_explanation", "").lower() or
               search_lower in term.get("veteran_explanation", "").lower()
        ]
    
    return {"terms": terms, "total": len(terms)}


@router.get("/terminology/{term_id}")
async def get_term(
    term_id: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get a specific term"""
    terms_data = load_help_content("terminology.json")
    
    if "terms" not in terms_data:
        raise HTTPException(status_code=404, detail="Term not found")
    
    for term in terms_data["terms"]:
        if term.get("id") == term_id or term.get("term", "").lower() == term_id.lower():
            return term
    
    raise HTTPException(status_code=404, detail="Term not found")


@router.get("/field-help/{field_path}")
async def get_field_help(
    field_path: str,
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Get contextual help for a specific form field"""
    field_help_data = load_help_content("field-help.json")
    
    if "fields" not in field_help_data:
        return {"help": None}
    
    # Support nested paths like "order.rate_type"
    field_map = {f.get("path"): f for f in field_help_data["fields"]}
    
    if field_path in field_map:
        return {"help": field_map[field_path]}
    
    # Try partial match
    for field in field_help_data["fields"]:
        if field_path.endswith(field.get("path", "")) or field.get("path", "").endswith(field_path):
            return {"help": field}
    
    return {"help": None}


@router.get("/search")
async def search_help(
    q: str = Query(..., description="Search query"),
    db: AsyncSession = Depends(get_db),
    current_user: User = Depends(get_current_user)
):
    """Search across all help content"""
    results = {
        "articles": [],
        "terms": [],
        "fields": []
    }
    
    search_lower = q.lower()
    
    # Search articles
    workflows = load_help_content("workflows.json")
    concepts = load_help_content("concepts.json")
    
    if "workflows" in workflows:
        for workflow in workflows["workflows"]:
            if (search_lower in workflow.get("title", "").lower() or
                search_lower in workflow.get("content", "").lower() or
                search_lower in workflow.get("description", "").lower()):
                results["articles"].append({**workflow, "type": "workflow"})
    
    if "concepts" in concepts:
        for concept in concepts["concepts"]:
            if (search_lower in concept.get("title", "").lower() or
                search_lower in concept.get("content", "").lower() or
                search_lower in concept.get("description", "").lower()):
                results["articles"].append({**concept, "type": "concept"})
    
    # Search terms
    terms_data = load_help_content("terminology.json")
    if "terms" in terms_data:
        for term in terms_data["terms"]:
            if (search_lower in term.get("term", "").lower() or
                search_lower in term.get("novice_explanation", "").lower() or
                search_lower in term.get("veteran_explanation", "").lower()):
                results["terms"].append(term)
    
    # Search field help
    field_help_data = load_help_content("field-help.json")
    if "fields" in field_help_data:
        for field in field_help_data["fields"]:
            if (search_lower in field.get("label", "").lower() or
                search_lower in field.get("help_text", "").lower()):
                results["fields"].append(field)
    
    return {
        "query": q,
        "results": results,
        "total": len(results["articles"]) + len(results["terms"]) + len(results["fields"])
    }

