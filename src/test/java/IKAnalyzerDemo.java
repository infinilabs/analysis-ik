/**
 *
 */

import java.io.IOException;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.CorruptIndexException;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.LockObtainFailedException;
import org.apache.lucene.store.RAMDirectory;
import org.wltea.analyzer.lucene.IKAnalyzer;
import org.wltea.analyzer.lucene.IKQueryParser;
import org.wltea.analyzer.lucene.IKSimilarity;

/**
 * @author linly
 *
 */
public class IKAnalyzerDemo {

	public static void main(String[] args){

		String fieldName = "text";

		String text = "IK Analyzer是一个结合词典分词和文法分词的中文分词开源工具包。它使用了全新的正向迭代最细粒度切分算法。";


		Analyzer analyzer = new IKAnalyzer();


		Directory directory = null;
		IndexWriter iwriter = null;
		IndexSearcher isearcher = null;
		try {

			directory = new RAMDirectory();
			iwriter = new IndexWriter(directory, analyzer, true , IndexWriter.MaxFieldLength.LIMITED);
			Document doc = new Document();
			doc.add(new Field("ID", "1111", Field.Store.YES, Field.Index.NOT_ANALYZED));
			doc.add(new Field(fieldName, text, Field.Store.YES, Field.Index.ANALYZED));
			iwriter.addDocument(doc);

			iwriter.close();


			isearcher = new IndexSearcher(directory);

			isearcher.setSimilarity(new IKSimilarity());

			String keyword = "中文分词工具包";


			Query query = IKQueryParser.parse(fieldName, keyword);


			TopDocs topDocs = isearcher.search(query , 5);
			System.out.println("命中：" + topDocs.totalHits);

			ScoreDoc[] scoreDocs = topDocs.scoreDocs;
			for (int i = 0; i < topDocs.totalHits; i++){
				Document targetDoc = isearcher.doc(scoreDocs[i].doc);
				System.out.println("内容：" + targetDoc.toString());
			}

		} catch (CorruptIndexException e) {
			e.printStackTrace();
		} catch (LockObtainFailedException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally{
			if(isearcher != null){
				try {
					isearcher.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			if(directory != null){
				try {
					directory.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
}