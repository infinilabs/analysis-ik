/**
 * 
 */
package org.wltea.analyzer.dic;

import java.util.HashMap;
import java.util.Map;


public class DictSegment {
	

	private static final Map<Character , Character> charMap = new HashMap<Character , Character>(16 , 0.95f);
	

	private static final int ARRAY_LENGTH_LIMIT = 3;
	

	private Character nodeChar;
	

	private Map<Character , DictSegment> childrenMap;
	

	private DictSegment[] childrenArray;
	


	private int storeSize = 0;
	

	private int nodeState = 0;	
	
	public DictSegment(Character nodeChar){
		if(nodeChar == null){
			throw new IllegalArgumentException("参数为空异常，字符不能为空");
		}
		this.nodeChar = nodeChar;
	}

    public int getDicNum(){
        if(charMap!=null)
        {
            return charMap.size();
        }
        return 0;
    }

	public Character getNodeChar() {
		return nodeChar;
	}
	
	/*
	 * 判断是否有下一个节点
	 */
	public boolean hasNextNode(){
		return  this.storeSize > 0;
	}
	
	/**
	 * 匹配词段
	 * @param charArray
	 * @return Hit
	 */
	public Hit match(char[] charArray){
		return this.match(charArray , 0 , charArray.length , null);
	}
	
	/**
	 * 匹配词段
	 * @param charArray
	 * @param begin
	 * @param length
	 * @return Hit 
	 */
	public Hit match(char[] charArray , int begin , int length){
		return this.match(charArray , begin , length , null);
	}
	
	/**
	 * 匹配词段
	 * @param charArray
	 * @param begin
	 * @param length
	 * @param searchHit
	 * @return Hit 
	 */
	public Hit match(char[] charArray , int begin , int length , Hit searchHit){
		
		if(searchHit == null){

			searchHit= new Hit();

			searchHit.setBegin(begin);
		}else{

			searchHit.setUnmatch();
		}

		searchHit.setEnd(begin);
		
		Character keyChar = new Character(charArray[begin]);
		DictSegment ds = null;
		

		DictSegment[] segmentArray = this.childrenArray;
		Map<Character , DictSegment> segmentMap = this.childrenMap;		
		

		if(segmentArray != null){

			for(DictSegment seg : segmentArray){
				if(seg != null && seg.nodeChar.equals(keyChar)){

					ds = seg;
				}
			}
		}else if(segmentMap != null){

			ds = (DictSegment)segmentMap.get(keyChar);
		}
		

		if(ds != null){			
			if(length > 1){

				return ds.match(charArray, begin + 1 , length - 1 , searchHit);
			}else if (length == 1){
				

				if(ds.nodeState == 1){

					searchHit.setMatch();
				}
				if(ds.hasNextNode()){

					searchHit.setPrefix();

					searchHit.setMatchedDictSegment(ds);
				}
				return searchHit;
			}
			
		}

		return searchHit;		
	}

	/**
	 * 加载填充词典片段
	 * @param charArray
	 */
	public void fillSegment(char[] charArray){
		this.fillSegment(charArray, 0 , charArray.length); 
	}
	
	/**
	 * 加载填充词典片段
	 * @param charArray
	 * @param begin
	 * @param length
	 */
	public synchronized void fillSegment(char[] charArray , int begin , int length){

		Character beginChar = new Character(charArray[begin]);
		Character keyChar = charMap.get(beginChar);

		if(keyChar == null){
			charMap.put(beginChar, beginChar);
			keyChar = beginChar;
		}
		

		DictSegment ds = lookforSegment(keyChar);

		if(length > 1){

			ds.fillSegment(charArray, begin + 1, length - 1);
		}else if (length == 1){

			ds.nodeState = 1;
		}

	}
	
	/**
	 * 查找本节点下对应的keyChar的segment
	 * 如果没有找到，则创建新的segment
	 * @param keyChar
	 * @return
	 */
	private DictSegment lookforSegment(Character keyChar){
		
		DictSegment ds = null;

		if(this.storeSize <= ARRAY_LENGTH_LIMIT){

			DictSegment[] segmentArray = getChildrenArray();			

			for(DictSegment segment : segmentArray){
				if(segment != null && segment.nodeChar.equals(keyChar)){

					ds =  segment;
					break;
				}
			}			

			if(ds == null){

				ds = new DictSegment(keyChar);				
				if(this.storeSize < ARRAY_LENGTH_LIMIT){

					segmentArray[this.storeSize] = ds;

					this.storeSize++;
				}else{


					Map<Character , DictSegment> segmentMap = getChildrenMap();

					migrate(segmentArray ,  segmentMap);

					segmentMap.put(keyChar, ds);

					this.storeSize++;

					this.childrenArray = null;
				}

			}			
			
		}else{

			Map<Character , DictSegment> segmentMap = getChildrenMap();

			ds = (DictSegment)segmentMap.get(keyChar);
			if(ds == null){

				ds = new DictSegment(keyChar);
				segmentMap.put(keyChar , ds);

				this.storeSize ++;
			}
		}

		return ds;
	}
	
	
	/**
	 * 获取数组容器
	 * 线程同步方法
	 */
	private DictSegment[] getChildrenArray(){
		if(this.childrenArray == null){
			synchronized(this){
				if(this.childrenArray == null){
					this.childrenArray = new DictSegment[ARRAY_LENGTH_LIMIT];
				}
			}
		}
		return this.childrenArray;
	}
	
	/**
	 * 获取Map容器
	 * 线程同步方法
	 */	
	private Map<Character , DictSegment> getChildrenMap(){
		if(this.childrenMap == null){
			synchronized(this){
				if(this.childrenMap == null){
					this.childrenMap = new HashMap<Character , DictSegment>(ARRAY_LENGTH_LIMIT * 2,0.8f);
				}
			}
		}
		return this.childrenMap;
	}
	
	/**
	 * 将数组中的segment迁移到Map中
	 * @param segmentArray
	 */
	private void migrate(DictSegment[] segmentArray , Map<Character , DictSegment> segmentMap){
		for(DictSegment segment : segmentArray){
			if(segment != null){
				segmentMap.put(segment.nodeChar, segment);
			}
		}
	}
	
}
