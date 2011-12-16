/**
 * 
 */
package org.wltea.analyzer;

import java.io.IOException;
import java.io.Reader;
import java.util.List;

import org.wltea.analyzer.cfg.Configuration;
import org.wltea.analyzer.help.CharacterHelper;
import org.wltea.analyzer.seg.ISegmenter;

public final class IKSegmentation{

	
	private Reader input;	
	private static final int BUFF_SIZE = 3072;
	private static final int BUFF_EXHAUST_CRITICAL = 48;
    private char[] segmentBuff;
	private Context context;
	private List<ISegmenter> segmenters;
    

	public IKSegmentation(Reader input){
		this(input , false);
	}
    

	public IKSegmentation(Reader input , boolean isMaxWordLength){
		this.input = input ;
		segmentBuff = new char[BUFF_SIZE];
		context = new Context(segmentBuff , isMaxWordLength);
		segmenters = Configuration.loadSegmenter();
	}

	public synchronized Lexeme next() throws IOException {
		if(context.getResultSize() == 0){
			/*
			 * 从reader中读取数据，填充buffer
			 * 如果reader是分次读入buffer的，那么buffer要进行移位处理
			 * 移位处理上次读入的但未处理的数据
			 */
			int available = fillBuffer(input);
			
            if(available <= 0){
            	context.resetContext();
                return null;
            }else{

        		int buffIndex = 0;
        		for( ; buffIndex < available ;  buffIndex++){

        			context.setCursor(buffIndex);

        			segmentBuff[buffIndex] = CharacterHelper.regularize(segmentBuff[buffIndex]);

        			for(ISegmenter segmenter : segmenters){
        				segmenter.nextLexeme(segmentBuff , context);
        			}
        			/*
        			 * 满足一下条件时，
        			 * 1.available == BUFF_SIZE 表示buffer满载
        			 * 2.buffIndex < available - 1 && buffIndex > available - BUFF_EXHAUST_CRITICAL表示当前指针处于临界区内
        			 * 3.!context.isBufferLocked()表示没有segmenter在占用buffer
        			 * 要中断当前循环（buffer要进行移位，并再读取数据的操作）
        			 */        			
        			if(available == BUFF_SIZE
        					&& buffIndex < available - 1   
        					&& buffIndex > available - BUFF_EXHAUST_CRITICAL
        					&& !context.isBufferLocked()){

        				break;
        			}
        		}
				
				for(ISegmenter segmenter : segmenters){
					segmenter.reset();
				}


        		context.setLastAnalyzed(buffIndex);

        		context.setBuffOffset(context.getBuffOffset() + buffIndex);

        		if(context.isMaxWordLength()){
        			context.excludeOverlap();
        		}

            	return buildLexeme(context.firstLexeme());
            }
		}else{

			return buildLexeme(context.firstLexeme());
		}	
	}

    private int fillBuffer(Reader reader) throws IOException{
    	int readCount = 0;
    	if(context.getBuffOffset() == 0){

    		readCount = reader.read(segmentBuff);
    	}else{
    		int offset = context.getAvailable() - context.getLastAnalyzed();
    		if(offset > 0){

    			System.arraycopy(segmentBuff , context.getLastAnalyzed() , this.segmentBuff , 0 , offset);
    			readCount = offset;
    		}

    		readCount += reader.read(segmentBuff , offset , BUFF_SIZE - offset);
    	}            	

    	context.setAvailable(readCount);
    	return readCount;
    }	

    private Lexeme buildLexeme(Lexeme lexeme){
    	if(lexeme != null){

			lexeme.setLexemeText(String.valueOf(segmentBuff , lexeme.getBegin() , lexeme.getLength()));
			return lexeme;
			
		}else{
			return null;
		}
    }

	public synchronized void reset(Reader input) {
		this.input = input;
		context.resetContext();
		for(ISegmenter segmenter : segmenters){
			segmenter.reset();
		}
	}

}
