

/*
 * JsMin
 * Javascript Compressor
 * http://www.crockford.com/
 * http://www.smallsharptools.com/
*/

dp.sh.Brushes.Groovy = function() {
    var keywords = 'abstract as assert in boolean break byte case catch char class const ' + 
		   'continue def default do double else enum extends ' +
                   'false final finally float for goto if implements import ' + 
                   'instanceof int interface long native new null ' +
                   'package private property protected public return ' + 
                   'short static strictfp super switch synchronized this throw throws true ' +
                   'transient try void volatile while';
    this.regexList =  [
    		{regex:new RegExp('"{3}[\\s\\S]*?\\"{3}','gm'),                         css: 'string'},  

    		{regex:dp.sh.RegexLib.SingleLineCComments,				css:'comment'},    		
    		{regex:dp.sh.RegexLib.MultiLineCComments,				css:'comment'},
    		{regex:dp.sh.RegexLib.SingleQuotedString,				css:'string'},
    		{regex:dp.sh.RegexLib.DoubleQuotedString,				css:'string'},    		
    		{regex:new RegExp('\\b([\\d]+(\\.[\\d]+)?|0x[a-f0-9]+)\\b', 'gi'),	css:'number'},
    		{regex:new RegExp('(?!\\@interface\\b)\\@[\\$\\w]+\\b', 'g'),		css:'annotation'},    		
    		{regex:new RegExp('\\@interface\\b', 'g'),				css:'keyword'},
    		{regex:new RegExp(this.GetKeywords(keywords), 'gm'),			css:'keyword'}];
    
    this.CssClass = 'dp-j';
    this.Style = '.dp-j .annotation { color: #646464; }' + '.dp-j .number { color: #C00000; }';
}
dp.sh.Brushes.Groovy.prototype = new dp.sh.Highlighter();
dp.sh.Brushes.Groovy.Aliases = ['groovy'];
