import { safeExternalUrl } from "../../../shared/utils/links.js";
export { safeExternalUrl };
/** 只根据当前论文自己的编号构建资源外链，不采用发现关系的evidenceUrl。 */
export function publicationResourceLinks(row) {
  const links = new Map();
  function add(value) {
    const text = String(value || "");
    for (const match of text.matchAll(/\bCRA[0-9]+\b/gi)) {
      const id = match[0].toUpperCase();
      links.set(id, { url: `https://ngdc.cncb.ac.cn/gsa/browse/${id}`, label: `GSA ${id}` });
    }
    for (const match of text.matchAll(/\bPRJ(?:CA|NA|EB|DA)[A-Z0-9]+\b/gi)) {
      const id = match[0].toUpperCase();
      links.set(id, { url: `https://ngdc.cncb.ac.cn/bioproject/browse/${id}`, label: `BioProject ${id}` });
    }
  }
  add(row?.gsaAccessions); add(row?.bioprojectAccession);
  let banks = row?.cncbBanksJson || row?.cncbBanks;
  try { if (typeof banks === "string") banks = JSON.parse(banks); } catch { banks = []; }
  if (Array.isArray(banks)) for (const bank of banks) { add(bank?.accession); add(bank?.link); }
  return [...links.values()];
}
/** 论文跳转优先PMID、DOI，随后使用明确的资源编号或后端返回的资源详情地址。 */
export function publicationExternalUrl(row) {
  if (row?.recordKind === "RESOURCE") return safeExternalUrl(row.resourceUrl || row.sourceUrl);
  const pmid = String(row?.pmid || (row?.identifierType === "PMID" ? row.identifierValue : "")).trim();
  if (/^\d+$/.test(pmid)) return `https://pubmed.ncbi.nlm.nih.gov/${pmid}/`;
  const doi = String(row?.doi || (row?.identifierType === "DOI" ? row.identifierValue : "")).trim()
    .replace(/^https?:\/\/(?:dx\.)?doi\.org\//i, "");
  if (/^10\.\d{4,9}\/\S+$/i.test(doi)) return `https://doi.org/${encodeURIComponent(doi)}`;
  const resource = publicationResourceLinks(row)[0];
  if (resource) return resource.url;
  const source = safeExternalUrl(row?.sourceUrl);
  if (!source) return null;
  // 不把采集API或未知来源地址当成可点击的论文地址。
  const url = new URL(source);
  return (url.hostname === "pubmed.ncbi.nlm.nih.gov" || url.hostname === "doi.org" ||
    url.hostname === "ngdc.cncb.ac.cn" && /^\/(?:gsa|bioproject)\/browse\/[A-Z0-9]+\/?$/i.test(url.pathname)) ? source : null;
}
export function publicationExternalLabel(row) {
  const url = publicationExternalUrl(row);
  if (url?.includes("/gsa/browse/")) return "查看GSA数据";
  if (url?.includes("/bioproject/browse/")) return "查看BioProject项目";
  if (row?.recordKind === "RESOURCE") return "查看资源";
  return url?.includes("pubmed.ncbi.nlm.nih.gov") ? "查看PubMed文献" : "查看文献";
}
