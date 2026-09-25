import logging
from typing import List
from fastapi import APIRouter, Depends
from app.schemas.schemas import (
    RetrievalQuery,
    LegalDocumentResult,
    RetrievalResponse,
    CaseLawQuery,
    CaseLawQueryResult,
    CaseLawRetrievalResponse,
)
from app.retrieval.hybrid_search import hybrid_search_engine
from app.rate_limiter import limit_search

logger = logging.getLogger(__name__)

router = APIRouter()

@router.post("/search", response_model=RetrievalResponse, dependencies=[Depends(limit_search)])
async def search_statutes(query: RetrievalQuery):
    """
    Server-side Hybrid Retrieval endpoint.
    - Sparse BM25 retrieval
    - Dense semantic scoring
    - Reciprocal Rank Fusion (k=60)
    - Metadata filtering (status != 'repealed', act filtering)
    - User jurisdiction boosting (1.25x)
    """
    raw_results = hybrid_search_engine.search(
        query=query.query,
        jurisdiction=query.jurisdiction,
        act_filter=query.act,
        limit=query.limit,
    )

    doc_results: List[LegalDocumentResult] = []
    for doc, score, mode in raw_results:
        doc_results.append(LegalDocumentResult(
            id=doc.id,
            act=doc.act,
            section=doc.section,
            title=doc.title,
            content=doc.content,
            jurisdiction=doc.jurisdiction,
            status=doc.status,
            relevance_score=round(score, 5),
            retrieval_mode=mode,
        ))

    return RetrievalResponse(
        query=query.query,
        results=doc_results,
        total=len(doc_results),
    )

@router.post("/search/case-law", response_model=CaseLawRetrievalResponse, dependencies=[Depends(limit_search)])
async def search_case_law_endpoint(query: CaseLawQuery):
    """
    Dedicated Case Law Retrieval endpoint.
    Maintains independent BM25 and Dense semantic scoring over Supreme Court precedents.
    """
    raw_results = hybrid_search_engine.search_case_law(
        query=query.query or "",
        legal_domain=query.domain,
        precedent_status=query.precedent_status,
        limit=query.limit,
    )

    case_results: List[CaseLawQueryResult] = []
    for chunk, score, mode in raw_results:
        case_results.append(CaseLawQueryResult(
            case_id=chunk.case_id,
            case_name=chunk.case_name,
            citation=chunk.citation,
            court=chunk.court,
            judgment_date=chunk.judgment_date,
            bench_strength=chunk.bench_strength,
            bench_judges=chunk.bench_judges,
            statutory_provisions=chunk.statutory_provisions,
            legal_domain=chunk.legal_domain,
            ratio_decidendi=chunk.ratio_decidendi,
            key_principles=chunk.key_principles,
            precedent_status=chunk.precedent_status,
            currentness_check=chunk.currentness_check,
            source_url=chunk.source_url,
            relevance_score=round(score, 5),
            retrieval_mode=mode,
        ))

    return CaseLawRetrievalResponse(
        query=query.query or "",
        results=case_results,
        total=len(case_results),
    )
