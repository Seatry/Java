import java.util.*;
 
class Vertex {
    public int x;
    public int t1, comp,low;
    public ArrayList<Vertex> bound = new ArrayList<>();
    public ArrayList<Vertex> vert = new ArrayList<>();
    public boolean bounded;
 
    public Vertex(int x) {
        this.x = x;
        this.t1 = 0;
        this.comp = -1;
    }
 
    public Vertex(ArrayList<Vertex> vert, int x) {
        this.x = x;
        this.vert = (ArrayList<Vertex>) vert.clone();
    }
}
 
public class GraphBase {
    private static int time = 1, count = 0;
    private static ArrayList<Vertex> comps = new ArrayList<>();
    private static ArrayList<Vertex> result = new ArrayList<>();
    private static ArrayList<Vertex> vertices = new ArrayList<>();
    private static Stack<Vertex> stack = new Stack<>();
 
    public static void main(String[] args) {
        Scanner in = new Scanner(System.in);
        int n = in.nextInt(), m = in.nextInt();
        for(int i = 0; i < n; i++)
            vertices.add(new Vertex(i));
        for(int i = 0; i < m; i++)
            vertices.get(in.nextInt()).bound.add(vertices.get(in.nextInt()));
 
        for(Vertex v : vertices)
            if(v.t1 == 0) visitVertex(v);
 
        comps.stream().forEach(c -> c.vert.stream().forEach(v ->
                v.bound.stream().filter(u -> v.comp != u.comp).forEach(u -> comps.get(u.comp).bounded = true)));
        comps.stream().filter(c -> !c.bounded).forEach(c ->
                result.add(Collections.min(c.vert,(a,b) -> Integer.compare(a.x,b.x))));
        Collections.sort(result,(a,b) -> Integer.compare(a.x,b.x));
        for(Vertex v : result) System.out.println(v.x);
    }
 
    public static void visitVertex(Vertex v) {
        v.t1 = v.low = time++;
        stack.push(v);
        for(Vertex u : v.bound) {
            if(u.t1 == 0) visitVertex(u);
            if(u.comp == -1 && v.low > u.low) v.low = u.low;
        }
        if(v.t1 == v.low) {
            Vertex u;
            ArrayList<Vertex> vert = new ArrayList<>();
            do {
                u = stack.pop();
                vert.add(u);
                u.comp = count;
            } while(u != v);
            comps.add(new Vertex(vert,count++));
        }
    }
}