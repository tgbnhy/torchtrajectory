package au.edu.rmit.bdm.clustering.Streaming;

public class VerticesEdge {
    int ver1;
    int ver2;
    public VerticesEdge(int ver1, int ver2){
        this.ver1 = ver1;
        this.ver2 = ver2;
    }

    @Override
    public int hashCode(){
        return String.valueOf(ver1).hashCode()+String.valueOf(ver2).hashCode();
    }

    @Override
    public boolean equals(Object _verticesEdge){
        if (!(_verticesEdge instanceof VerticesEdge)) return false;
        VerticesEdge verticesEdge = (VerticesEdge)_verticesEdge;
        return (ver1 == verticesEdge.ver1  && ver2 == verticesEdge.ver2)||
                (ver1 == verticesEdge.ver2 && ver2 == verticesEdge.ver1);
    }
}
