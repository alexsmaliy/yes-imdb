package com.alexsmaliy.yesimdb.index;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.LowerCaseFilter;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.WhitespaceTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PositionIncrementAttribute;

import java.io.IOException;
import java.util.regex.Pattern;

public class NameAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String field) {
        Tokenizer tokenizer = new WhitespaceTokenizer();
        TokenStream tokenStream = new LowerCaseFilter(new NameTokenFilter(tokenizer));
        return new TokenStreamComponents(tokenizer, tokenStream);
    }

    private static class NameTokenFilter extends TokenFilter {
        private static final Pattern SINGLE_INITIAL_TOKEN =
            Pattern.compile("\\p{Alnum}\\.?", Pattern.CASE_INSENSITIVE);

        protected CharTermAttribute charTermAttribute =
            addAttribute(CharTermAttribute.class);
        protected PositionIncrementAttribute positionIncrementAttribute =
            addAttribute(PositionIncrementAttribute.class);

        NameTokenFilter(TokenStream tokenStream) {
            super(tokenStream);
        }

        private boolean isMeaningfulNameToken(String token) {
            return !token.isEmpty()
                && !SINGLE_INITIAL_TOKEN.matcher(token).matches();
        }

        @Override
        public boolean incrementToken() throws IOException {
            String nextToken = null;
            while (nextToken == null) {
                if ( !input.incrementToken()) {
                    return false;
                }
                String currentTokenInStream =
                    input.getAttribute(CharTermAttribute.class).toString().trim();
                if (isMeaningfulNameToken(currentTokenInStream)) {
                    nextToken = currentTokenInStream;
                }
            }
            charTermAttribute.setEmpty().append(nextToken);
            positionIncrementAttribute.setPositionIncrement(1);
            return true;
        }
    }
}
