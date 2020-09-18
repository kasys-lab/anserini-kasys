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


public class LMABSExtractor<T> implements FeatureExtractor<T> {
  //ログの表示に関する設定
  private static final Logger LOG = LogManager.getLogger(BM25FeatureExtractor.class);

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

  private float delta = 0.7f;
  private float lambda = 0.1f;


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
    long docSize  = 0L;
    long uniqueDocSize = 0L;

    //Mapに単語とその単語の文章中の出現回数を格納 
    Map<String, Long> termMap = new HashMap<>();
    try {
      TermsEnum termsEnum = terms.iterator();
      while (termsEnum.next() != null) {
        String termString = termsEnum.term().utf8ToString();
        docSize += termsEnum.totalTermFreq();
        termMap.put(termString, termsEnum.totalTermFreq());
        if (queryTokens.contains(termString)) {
          termFreqMap.put(termString, termsEnum.totalTermFreq());
        }
      }
    } catch (IOException e) {
      LOG.warn("Unable to retrieve termsEnum, treating as 0");
    }

    uniqueDocSize = termMap.size();
    float score = 0.0f;
    float mae = 0.0f;
    float ato = 0.0f;
    float atoue = 0.0f;
    float atoshita = 0.0f;
    long tf = 0L;




     // それぞれの単語の文章中の出現回数を
    for (String token : queryTokens) {
      long termFreq = termFreqMap.containsKey(token) ? termFreqMap.get(token) : 0;
      if (termFreq - delta < 0){
        termFreq = 0L;
      }
      try {
        //コーパス全体にある単語が出る回数
        tf = 0L;
        tf = reader.totalTermFreq(new Term(LuceneDocumentGenerator.FIELD_BODY, token));
      } catch (IOException e) {
        LOG.warn("Unable to retrieve document frequencies.");
        tf = 0;
      }

      mae = (float)sumTotalTermFreq * (float)termFreq + (float)delta * (float)uniqueDocSize * (float)tf;
      ato = (float)docSize * (float)sumTotalTermFreq;
      // mae = (float)termFreq / (float)docSize;
      // ato = ((float)delta * (float)uniqueDocSize / (float)docSize) * ((float)tf / (float)sumTotalTermFreq);
      score +=  Math.log(mae) - Math.log(ato);
    }
    return score;
  }

  @Override
  public String getName() {
    return "LM.ABS";
  }
}
