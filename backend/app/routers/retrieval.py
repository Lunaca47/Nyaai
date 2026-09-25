import logging
from typing import List
from fastapi import APIRouter, Depends
from app.schemas.schemas import RetrievalQuery, LegalDocumentResult, RetrievalResponse
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
