/*
 * Anserini: A Lucene toolkit for replicable information retrieval research
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.anserini.ltr.feature.base;

import io.anserini.ltr.feature.FeatureExtractor;
import io.anserini.rerank.RerankerContext;
import org.apache.logging.log4j.Logger;
import org.apache.logging.log4j.LogManager;
import io.anserini.index.generator.LuceneDocumentGenerator;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.index.MultiTerms;


import java.util.List;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.Set;


public class LMDIRExtractor<T> implements FeatureExtractor<T> {
  //ログの表示に関する設定
  private static final Logger LOG = LogManager.getLogger(BM25FeatureExtractor.class);

  // 単語の出現回数をMap型で格納
  // public static Map<String, Integer> getDocFreqs(IndexReader reader, List<String> queryTokens, String field) throws IOException {
  //   // Must retrieve from multifields
  //   for (String queryToken : queryTokens) {
  //     docFreqs.put(queryToken, reader.docFreq(new Term(field, queryToken)));
  //   }
  //   return docFreqs;
  // }

    // コーパス中の総単語数
  private long getSumTermFrequency(IndexReader reader, String fieldName) {
    Terms collectionTermVector = null;
    try {
      collectionTermVector = MultiTerms.getTerms(reader, fieldName);
      long totalTermFreq = collectionTermVector.getSumTotalTermFreq();
      return totalTermFreq;
    } catch (IOException e) {
      LOG.warn("Unable to get total term frequency, it might not be indexed");
    }
    return 0;
  }

  private long myu = 2000L;

  @Override
  public float extract(Document doc, Terms terms, RerankerContext<T> context) {
    //文章頻度をカウントするMapを定義
    // Map<String, Integer> docFreqMap = null;
    Map<String, Long> termFreqMap = new HashMap<>();
    IndexReader reader = context.getIndexSearcher().getIndexReader();

     // クエリを取得
    Set<String> queryTokens = new HashSet<>(context.getQueryTokens());

    // コーパス中の総単語数
    long sumTotalTermFreq = getSumTermFrequency(reader, LuceneDocumentGenerator.FIELD_BODY);

    // //文章の総単語数
    // float docsize;
    // try {
    //   docsize = (float)terms.getSumTotalTermFreq();
    //   if (docsize == -1) {
    //     // try to iterate over the terms
    //     TermsEnum termsEnum = terms.iterator();
    //     docsize = 0.0f;
    //     while (termsEnum.next()!= null) {
    //       docsize += termsEnum.totalTermFreq();
    //     }
    //   }
    // } catch (IOException e) {
    //   docsize = 0.0f;
    // }

    long docSize  = 0L;

    //Mapに単語とその単語の文章中の出現回数を格納 
    // Map<String, Long> termFreqMap = new HashMap<>();
    try {
      TermsEnum termsEnum = terms.iterator();
      while (termsEnum.next() != null) {
        String termString = termsEnum.term().utf8ToString();
        docSize += termsEnum.totalTermFreq();
        if (queryTokens.contains(termString)) {
          termFreqMap.put(termString, termsEnum.totalTermFreq());
        }
      }
    } catch (IOException e) {
      LOG.warn("Unable to retrieve termsEnum, treating as 0");
    }

    float score = 0.0f;
    float bunnshi1 = 0.0f;
    float bunnshi2 = 0.0f;
    float bunnbo = 0.0f;

     // それぞれの単語の文章中の出現回数を
    for (String token : queryTokens) {
      long termFreq = termFreqMap.containsKey(token) ? termFreqMap.get(token) : 0;
      long tf;
      try {
        //コーパス全体にある単語が出る回数
        tf = reader.totalTermFreq(new Term(LuceneDocumentGenerator.FIELD_BODY, token));
      } catch (IOException e) {
        LOG.warn("Unable to retrieve document frequencies.");
        tf = 0;
      }
      bunnshi1 = (float)termFreq * (float)sumTotalTermFreq + (myu * (float)tf);
      bunnshi2 = (float)sumTotalTermFreq;
      bunnbo = docSize + myu;
      score += Math.log(bunnshi1) - Math.log(bunnshi2) - Math.log(bunnbo);
    }

    return score;
  }

  @Override
  public String getName() {
    return "LM.DIR";
  }
}
