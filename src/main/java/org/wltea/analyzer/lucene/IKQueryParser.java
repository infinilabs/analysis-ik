/**
 * 
 */
package org.wltea.analyzer.lucene;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.BooleanClause.Occur;

import org.wltea.analyzer.IKSegmentation;
import org.wltea.analyzer.Lexeme;


public final class IKQueryParser {
	
	
	private static ThreadLocal<Map<String , TokenBranch>> keywordCacheThreadLocal
			= new ThreadLocal<Map<String , TokenBranch>>();
	
	
	private static boolean isMaxWordLength = false;


	public static void setMaxWordLength(boolean isMaxWordLength) {
		IKQueryParser.isMaxWordLength = isMaxWordLength ;
	}
	

	private static Query optimizeQueries(List<Query> queries){	

		if(queries.size() == 0){
			return null;
		}else if(queries.size() == 1){
			return queries.get(0);
		}else{
			BooleanQuery mustQueries = new BooleanQuery();
			for(Query q : queries){
				mustQueries.add(q, Occur.MUST);
			}
			return mustQueries;
		}			
	}
	

	private static Map<String , TokenBranch> getTheadLocalCache(){
		Map<String , TokenBranch> keywordCache = keywordCacheThreadLocal.get();
		if(keywordCache == null){
			 keywordCache = new HashMap<String , TokenBranch>(4);
			 keywordCacheThreadLocal.set(keywordCache);
		}
		return keywordCache;
	}
	

	private static TokenBranch getCachedTokenBranch(String query){
		Map<String , TokenBranch> keywordCache = getTheadLocalCache();
		return keywordCache.get(query);
	}
	

	private static void cachedTokenBranch(String query , TokenBranch tb){
		Map<String , TokenBranch> keywordCache = getTheadLocalCache();
		keywordCache.put(query, tb);
	}
		
	

	private static Query _parse(String field , String query) throws IOException{
		if(field == null){
			throw new IllegalArgumentException("parameter \"field\" is null");
		}

		if(query == null || "".equals(query.trim())){
			return new TermQuery(new Term(field));
		}
		

		TokenBranch root = getCachedTokenBranch(query);
		if(root != null){
			return optimizeQueries(root.toQueries(field)); 
		}else{

			root = new TokenBranch(null);		

			StringReader input = new StringReader(query.trim());
			IKSegmentation ikSeg = new IKSegmentation(input , isMaxWordLength);
			for(Lexeme lexeme = ikSeg.next() ; lexeme != null ; lexeme = ikSeg.next()){

				root.accept(lexeme);
			}

			cachedTokenBranch(query , root);
			return optimizeQueries(root.toQueries(field));
		}
	}
	

	public static Query parse(String field , String query) throws IOException{
		if(field == null){
			throw new IllegalArgumentException("parameter \"field\" is null");
		}
		String[] qParts = query.split("\\s");
		if(qParts.length > 1){			
			BooleanQuery resultQuery = new BooleanQuery();
			for(String q : qParts){

				if("".equals(q)){
					continue;
				}
				Query partQuery = _parse(field , q);
				if(partQuery != null && 
				          (!(partQuery instanceof BooleanQuery) || ((BooleanQuery)partQuery).getClauses().length>0)){
					resultQuery.add(partQuery, Occur.SHOULD); 
				}
			}
			return resultQuery;
		}else{
			return _parse(field , query);
		}
	}
	

	public static Query parseMultiField(String[] fields , String query) throws IOException{
		if(fields == null){
			throw new IllegalArgumentException("parameter \"fields\" is null");
		}		
		BooleanQuery resultQuery = new BooleanQuery();		
		for(String field : fields){
			if(field != null){
				Query partQuery = parse(field , query);
				if(partQuery != null && 
				          (!(partQuery instanceof BooleanQuery) || ((BooleanQuery)partQuery).getClauses().length>0)){
					resultQuery.add(partQuery, Occur.SHOULD); 
				}
			}			
		}		
		return resultQuery;
	}
	

	public static Query parseMultiField(String[] fields , String query ,  BooleanClause.Occur[] flags) throws IOException{
		if(fields == null){
			throw new IllegalArgumentException("parameter \"fields\" is null");
		}
		if(flags == null){
			throw new IllegalArgumentException("parameter \"flags\" is null");
		}
		
		if (flags.length != fields.length){
		      throw new IllegalArgumentException("flags.length != fields.length");
		}		
		
		BooleanQuery resultQuery = new BooleanQuery();		
		for(int i = 0; i < fields.length; i++){
			if(fields[i] != null){
				Query partQuery = parse(fields[i] , query);
				if(partQuery != null && 
				          (!(partQuery instanceof BooleanQuery) || ((BooleanQuery)partQuery).getClauses().length>0)){
					resultQuery.add(partQuery, flags[i]); 
				}
			}			
		}		
		return resultQuery;
	}
	

	public static Query parseMultiField(String[] fields , String[] queries) throws IOException{
		if(fields == null){
			throw new IllegalArgumentException("parameter \"fields\" is null");
		}				
		if(queries == null){
			throw new IllegalArgumentException("parameter \"queries\" is null");
		}				
		if (queries.length != fields.length){
		      throw new IllegalArgumentException("queries.length != fields.length");
		}
		BooleanQuery resultQuery = new BooleanQuery();		
		for(int i = 0; i < fields.length; i++){
			if(fields[i] != null){
				Query partQuery = parse(fields[i] , queries[i]);
				if(partQuery != null && 
				          (!(partQuery instanceof BooleanQuery) || ((BooleanQuery)partQuery).getClauses().length>0)){
					resultQuery.add(partQuery, Occur.SHOULD); 
				}
			}			
		}		
		return resultQuery;
	}


	public static Query parseMultiField(String[] fields , String[] queries , BooleanClause.Occur[] flags) throws IOException{
		if(fields == null){
			throw new IllegalArgumentException("parameter \"fields\" is null");
		}				
		if(queries == null){
			throw new IllegalArgumentException("parameter \"queries\" is null");
		}
		if(flags == null){
			throw new IllegalArgumentException("parameter \"flags\" is null");
		}
		
	    if (!(queries.length == fields.length && queries.length == flags.length)){
	        throw new IllegalArgumentException("queries, fields, and flags array have have different length");
	    }

	    BooleanQuery resultQuery = new BooleanQuery();		
		for(int i = 0; i < fields.length; i++){
			if(fields[i] != null){
				Query partQuery = parse(fields[i] , queries[i]);
				if(partQuery != null && 
				          (!(partQuery instanceof BooleanQuery) || ((BooleanQuery)partQuery).getClauses().length>0)){
					resultQuery.add(partQuery, flags[i]); 
				}
			}			
		}		
		return resultQuery;
	}	

	private static class TokenBranch{
		
		private static final int REFUSED = -1;
		private static final int ACCEPTED = 0;
		private static final int TONEXT = 1;
		
		private int leftBorder;
		private int rightBorder;
		private Lexeme lexeme;
		private List<TokenBranch> acceptedBranchs;
		private TokenBranch nextBranch;
		
		TokenBranch(Lexeme lexeme){
			if(lexeme != null){
				this.lexeme = lexeme;

				this.leftBorder = lexeme.getBeginPosition();
				this.rightBorder = lexeme.getEndPosition();
			}
		}
		
		public int getLeftBorder() {
			return leftBorder;
		}

		public int getRightBorder() {
			return rightBorder;
		}

		public Lexeme getLexeme() {
			return lexeme;
		}

		public List<TokenBranch> getAcceptedBranchs() {
			return acceptedBranchs;
		}

		public TokenBranch getNextBranch() {
			return nextBranch;
		}

		public int hashCode(){
			if(this.lexeme == null){
				return 0;
			}else{
				return this.lexeme.hashCode() * 37;
			}
		}
		
		public boolean equals(Object o){			
			if(o == null){
				return false;
			}		
			if(this == o){
				return true;
			}
			if(o instanceof TokenBranch){
				TokenBranch other = (TokenBranch)o;
				if(this.lexeme == null ||
						other.getLexeme() == null){
					return false;
				}else{
					return this.lexeme.equals(other.getLexeme());
				}
			}else{
				return false;
			}			
		}	
		

		boolean accept(Lexeme _lexeme){
			
			/*
			 * 检查新的lexeme 对当前的branch 的可接受类型
			 * acceptType : REFUSED  不能接受
			 * acceptType : ACCEPTED 接受
			 * acceptType : TONEXT   由相邻分支接受 
			 */			
			int acceptType = checkAccept(_lexeme);			
			switch(acceptType){
			case REFUSED:

				return false;
				
			case ACCEPTED : 
				if(acceptedBranchs == null){

					acceptedBranchs = new ArrayList<TokenBranch>(2);
					acceptedBranchs.add(new TokenBranch(_lexeme));					
				}else{
					boolean acceptedByChild = false;

					for(TokenBranch childBranch : acceptedBranchs){
						acceptedByChild = childBranch.accept(_lexeme) || acceptedByChild;
					}

					if(!acceptedByChild){
						acceptedBranchs.add(new TokenBranch(_lexeme));
					}					
				}

				if(_lexeme.getEndPosition() > this.rightBorder){
					this.rightBorder = _lexeme.getEndPosition();
				}
				break;
				
			case TONEXT : 

				if(this.nextBranch == null){

					this.nextBranch = new TokenBranch(null);
				}
				this.nextBranch.accept(_lexeme);
				break;
			}

			return true;
		}
		

		List<Query> toQueries(String fieldName){			
			List<Query> queries = new ArrayList<Query>(1);			

			if(lexeme != null){
				queries.add(new TermQuery(new Term(fieldName , lexeme.getLexemeText())));
			}			

			if(acceptedBranchs != null && acceptedBranchs.size() > 0){
				if(acceptedBranchs.size() == 1){
					Query onlyOneQuery = optimizeQueries(acceptedBranchs.get(0).toQueries(fieldName));
					if(onlyOneQuery != null){
						queries.add(onlyOneQuery);
					}					
				}else{
					BooleanQuery orQuery = new BooleanQuery();
					for(TokenBranch childBranch : acceptedBranchs){
						Query childQuery = optimizeQueries(childBranch.toQueries(fieldName));
						if(childQuery != null){
							orQuery.add(childQuery, Occur.SHOULD);
						}
					}
					if(orQuery.getClauses().length > 0){
						queries.add(orQuery);
					}
				}
			}			

			if(nextBranch != null){				
				queries.addAll(nextBranch.toQueries(fieldName));
			}
			return queries;	
		}
		

		private int checkAccept(Lexeme _lexeme){
			int acceptType = 0;
			
			if(_lexeme == null){
				throw new IllegalArgumentException("parameter:lexeme is null");
			}
			
			if(null == this.lexeme){
				if(this.rightBorder > 0
						&& _lexeme.getBeginPosition() >= this.rightBorder){

					acceptType = TONEXT;
				}else{
					acceptType = ACCEPTED;
				}				
			}else{
				
				if(_lexeme.getBeginPosition() < this.lexeme.getBeginPosition()){

					acceptType = REFUSED;
				}else if(_lexeme.getBeginPosition() >= this.lexeme.getBeginPosition()
							&& _lexeme.getBeginPosition() < this.lexeme.getEndPosition()){

					acceptType = REFUSED;
				}else if(_lexeme.getBeginPosition() >= this.lexeme.getEndPosition()
							&& _lexeme.getBeginPosition() < this.rightBorder){

					acceptType = ACCEPTED;
				}else{

					acceptType=  TONEXT;
				}
			}
			return acceptType;
		}
	
	}
}
