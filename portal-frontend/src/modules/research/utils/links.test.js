import test from "node:test";
import assert from "node:assert/strict";
import { publicationExternalUrl, publicationResourceLinks, publicationExternalLabel } from "./links.js";
test("PMID and DOI keep priority while resource buttons remain available", () => {
  const row = { pmid: "41769996", doi: "10.1128/abc", gsaAccessions: "CRA042678", bioprojectAccession: "PRJCA063830" };
  assert.equal(publicationExternalUrl(row), "https://pubmed.ncbi.nlm.nih.gov/41769996/");
  assert.equal(publicationResourceLinks(row).length, 2);
  assert.match(publicationExternalUrl({doi: row.doi, gsaAccessions: row.gsaAccessions}), /^https:\/\/doi.org\//);
});
test("native accessions and old cncbBanks support resource fallback and deduplication", () => {
  const row = { gsaAccessions: "CRA042678", cncbBanksJson: JSON.stringify([{accession: "CRA042678"}, {link: "https://ngdc.cncb.ac.cn/bioproject/browse/PRJCA063830"}]) };
  assert.equal(publicationExternalUrl(row), "https://ngdc.cncb.ac.cn/gsa/browse/CRA042678");
  assert.equal(publicationExternalLabel(row), "查看GSA数据");
  assert.equal(publicationResourceLinks(row).length, 2);
  assert.equal(publicationExternalLabel({bioprojectAccession: "PRJCA063830"}), "查看BioProject项目");
});
test("discovery evidence is never treated as the related paper's own URL", () => {
  assert.equal(publicationExternalUrl({evidenceUrl: "https://ngdc.cncb.ac.cn/bioproject/browse/PRJCA063830"}), null);
  assert.equal(publicationExternalUrl({sourceUrl: "javascript:alert(1)"}), null);
  assert.equal(publicationExternalUrl({sourceUrl: "https://ngdc.cncb.ac.cn/openlb/api/pmid?pmid=41769996"}), null);
  assert.equal(publicationExternalUrl({cncbBanksJson: "bad"}), null);
});
