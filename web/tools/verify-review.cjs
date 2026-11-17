"use strict";
const fs=require("node:fs");
const {buildReviewQueue}=require("./review-queue.cjs");
const {validatePacket}=require("../review.js");
if(!process.argv[2])throw Error("Usage: node web/tools/verify-review.cjs review.json");
const queue=buildReviewQueue(),packet=validatePacket(JSON.parse(fs.readFileSync(process.argv[2],"utf8")),queue);
const counts={approved:0,changes:0,hold:0};for(const d of packet.decisions)counts[d.status]++;
console.log(JSON.stringify({snapshot:queue.snapshot,items:queue.items.length,recorded:packet.decisions.length,counts,identityVerified:false,releaseAuthorized:false},null,2));
