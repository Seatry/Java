/**
 * Created by sasha on 07.04.16.
 */
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.Scanner;
import java.util.function.IntPredicate;
import java.util.*;

class Position {
    String text;
    int index, line, col;

    public Position(String text) {
        this(text, 0, 1, 1);
    }

    private Position(String text, int index, int line, int col) {
        this.text = text;
        this.index = index;
        this.line = line;
        this.col = col;
    }

    public int getChar() {
        return index < text.length() ? text.codePointAt(index) : -1;
    }

    public boolean satisfies(IntPredicate p) {
        return p.test(getChar());
    }

    public Position skip() {
        int c = getChar();
        switch (c) {
            case -1:
                return this;
            case '\n':
                return new Position(text, index+1, line+1, 1);
            default:
                return new Position(text, index + (c > 0xFFFF ? 2 : 1), line, col+1);
        }
    }

    public Position skipWhile(IntPredicate p) {
        Position pos = this;
        while (pos.satisfies(p)) pos = pos.skip();
        return pos;
    }

    public String toString() {
        return String.format("(%d, %d)", line, col);
    }
}

class SyntaxError extends Exception {
    public SyntaxError(Position pos, String msg) {
        super(String.format("syntax error"));
    }
}

enum Tag {
    IDENT,
    NUMBER,
    LPAREN,
    RPAREN,
    PLUS,
    MINUS,
    MUL,
    DIV,
    EQUAL,
    COM,
    END_OF_TEXT;

    public String toString() {
        switch (this) {
            case IDENT: return "identifier";
            case NUMBER: return "number";
            case PLUS: return "'+'";
            case MINUS: return "'-'";
            case MUL: return "'*'";
            case DIV: return "'/'";
            case LPAREN: return "'('";
            case RPAREN: return "')'";
            case EQUAL: return "'='";
            case END_OF_TEXT: return "end of text";
            case COM: return "',";
        }
        throw new RuntimeException("error");
    }
}

class Token {
    private Tag tag;
    Position start, follow;
    public String var,text;

    public Token(String text) throws SyntaxError {
        this(new Position(text));
    }

    private Token(Position cur) throws SyntaxError {
        start = cur.skipWhile(Character::isWhitespace);
        this.text = start.text;
        follow = start.skip();
        switch (start.getChar()) {
            case -1:
                tag = Tag.END_OF_TEXT;
                break;
            case '(':
                tag = Tag.LPAREN;
                break;
            case ')':
                tag = Tag.RPAREN;
                break;
            case '+':
                tag = Tag.PLUS;
                break;
            case '-':
                tag = Tag.MINUS;
                break;
            case '*':
                tag = Tag.MUL;
                break;
            case '/':
                tag = Tag.DIV;
                break;
            case '=':
                tag = Tag.EQUAL;
                break;
            case ',':
                tag = Tag.COM;
                break;
            default:
                if (start.satisfies(Character::isLetter)) {
                    follow = follow.skipWhile(Character::isLetterOrDigit);
                    var = text.substring(start.index,follow.index);
                    tag = Tag.IDENT;
                } else if (start.satisfies(Character::isDigit)) {
                    follow = follow.skipWhile(Character::isDigit);
                    if (follow.satisfies(Character::isLetter)) {
                        throw new SyntaxError(follow, "error");
                    }
                    tag = Tag.NUMBER;
                } else {
                    throwError("error");
                }
        }
    }

    public void throwError(String msg) throws SyntaxError {
        throw new SyntaxError(start, msg);
    }

    public boolean matches(Tag ...tags) {
        for(Tag t : tags) if(tag == t ) return true;
        return false;
        // return Arrays.stream(tags).anyMatch(t -> tag == t);
    }

    public Token next() throws SyntaxError {
        return new Token(follow);
    }
}

class Graph {
    public ArrayList<Vertex> vertices = new ArrayList<>();
    public HashMap<Vertex,String> formul_map = new HashMap<>();
}

class Vertex {
    public ArrayList<String> var;
    public HashSet<String> bound = new HashSet<>();
    public int t1,t2,c;

    public Vertex(ArrayList<String> var) {
        this.var = (ArrayList<String>) var.clone();
        t1 = 0;
        t2 = 0;
        c = 0;
    }

}

class Parser {
    private Token sym;
    private String text;
    private ArrayList<String> formuls = new ArrayList<>();
    private ArrayList<String> cur_formula = new ArrayList<>();
    private int cur = 0;
    private HashSet<String> calls = new HashSet<>();
    private Graph graph = new Graph();
    private String[] lines = new String[21000];

    private void expect(Tag tag) throws SyntaxError {
        if (!sym.matches(tag)) {
            sym.throwError("syntax error");
        }
        sym = sym.next();
    }

    public Parser(String text) {
        this.text = text;
        lines = text.split("\n");
    }

    public Graph parsing() {
        try {
            sym = new Token(text);
            for(String line : lines)
                parseFormula();
            for(String call : calls) {
                if(!formuls.contains(call)) sym.throwError("syntax error");
            }
            expect(Tag.END_OF_TEXT);
            return graph;
        }
        catch (SyntaxError e) {
            graph.vertices.clear();
            return graph;
        }
    }
    private void parseFormula() throws SyntaxError {
        if(sym.matches(Tag.IDENT)) {
            cur_formula.clear();
            int count1 = parseIdentList();
            graph.vertices.add(new Vertex(cur_formula));
            graph.formul_map.put(graph.vertices.get(cur), lines[cur]);
            expect(Tag.EQUAL);
            int count2 = parseExprList();
            if (count1 != count2) sym.throwError("syntax error");
            cur++;
        }
    }

    private int parseIdentList() throws SyntaxError {
        int count = 0;
        if(sym.matches(Tag.IDENT)) {
            if(formuls.contains(sym.var)) sym.throwError("syntax error");
            cur_formula.add(sym.var);
            formuls.add(sym.var);
            sym = sym.next();
            count = parseIdentList2(count);
        }
        else {
            sym.throwError("syntax error");
        }
        return count;
    }

    private int parseIdentList2(int count) throws SyntaxError {
        if(sym.matches(Tag.COM)) {
            sym = sym.next();
            count+= parseIdentList() + 1;
        }
        return count;
    }

    private int parseExprList() throws SyntaxError {
        int count = 0;
        if(sym.matches(Tag.IDENT,Tag.NUMBER,Tag.MINUS,Tag.LPAREN)) {
            parseExpr();
            count = parseExprList2(count);
        } else {
            sym.throwError("syntax error");
        }
        return count;
    }

    private int parseExprList2(int count) throws SyntaxError {
        if(sym.matches(Tag.COM)) {
            sym = sym.next();
            count+= parseExprList() + 1;
        }
        return count;
    }
    private  void parseExpr() throws SyntaxError {
        parseTerm();
        parseExpr2();
    }
    private void parseExpr2() throws SyntaxError {
        if (sym.matches(Tag.PLUS)) {
            sym = sym.next();
            parseTerm();
            parseExpr2();
        } else if (sym.matches(Tag.MINUS)) {
            sym = sym.next();
            parseTerm();
            parseExpr2();
        }
    }

    private void parseTerm() throws SyntaxError {
        parseFact();
        parseTerm2();
    }

    private void parseTerm2() throws SyntaxError {
        if (sym.matches(Tag.MUL)) {
            sym = sym.next();
            parseFact();
            parseTerm2();
        } else if (sym.matches(Tag.DIV)) {
            sym = sym.next();
            parseFact();
            parseTerm2();
        }
    }

    private void parseFact() throws SyntaxError {
        if (sym.matches(Tag.NUMBER)) {
            sym = sym.next();
        } else if (sym.matches(Tag.IDENT)) {
            graph.vertices.get(cur).bound.add(sym.var);
            calls.add(sym.var);
            sym = sym.next();
        } else if (sym.matches(Tag.LPAREN)) {
            sym = sym.next();
            parseExpr();
            expect(Tag.RPAREN);
        } else if (sym.matches(Tag.MINUS)) {
            sym = sym.next();
            parseFact();
        } else {
            sym.throwError("syntax error");
        }
    }
}

class CycleError extends Exception {
    public CycleError(String msg) {
        super(String.format(msg));
    }
}
public class FormulaOrder {
    private static int time = 1;
    private static ArrayList<String> result = new ArrayList<>();

    public static void main(String[] args) throws CycleError{
        Scanner in = new Scanner(System.in);
        in.useDelimiter("\\Z");
        String text = in.next();
        Parser parser = new Parser(text);
        Graph graph = parser.parsing();
        if(graph.vertices.isEmpty()) System.out.println("syntax error");
        else {
            try {
                dfs(graph,graph.vertices);
                for(String s : result) System.out.println(s);
            } catch (CycleError e) {
                System.out.println(e.getMessage());
            }
        }
    }

    public static void dfs(Graph graph, ArrayList<Vertex> vertices) throws CycleError {
        for(Vertex v : vertices)
            if(v.t1 == 0) visitVertex(graph, vertices,v);
    }

    public static void visitVertex(Graph graph, ArrayList<Vertex> vertices, Vertex v) throws CycleError{
        v.t1 = time++;
        for(String s : v.bound) {
            Vertex u = new Vertex(new ArrayList<String>());
            for(Vertex y : vertices)
                if(y.var.contains(s)) {
                    u=y;
                    u.c++;
                }
            if (u.t2 == 0 && u.t1!=0 ) throw new CycleError("cycle");
            if(u.t1 == 0) visitVertex(graph,vertices,u);
        }
        result.add(graph.formul_map.get(v));
        v.t2 = time++;
    }

}