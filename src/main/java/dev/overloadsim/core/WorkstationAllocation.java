package dev.overloadsim.core;

/** Counted shapeless allocation; null means no feasible simultaneous allocation. */
public final class WorkstationAllocation {
    private WorkstationAllocation() {}
    public static int[] allocate(int[] available,int[] demand,boolean[][] matches,int parallel) {
        if(parallel<=0||available.length>9||demand.length>9||matches.length!=demand.length)return null;
        long capacity=0,required=0;
        for(int count:available){if(count<0)return null;capacity+=count;}
        for(int i=0;i<demand.length;i++){
            if(demand[i]<=0||matches[i].length!=available.length)return null;
            required+=(long)demand[i]*parallel;
        }
        if(required>capacity)return null;
        int source=0,firstRequirement=1,firstSlot=1+demand.length,sink=firstSlot+available.length,n=sink+1;
        long[][] residual=new long[n][n];
        for(int i=0;i<demand.length;i++){
            residual[source][firstRequirement+i]=(long)demand[i]*parallel;
            for(int slot=0;slot<available.length;slot++)if(matches[i][slot])residual[firstRequirement+i][firstSlot+slot]=capacity;
        }
        for(int slot=0;slot<available.length;slot++)residual[firstSlot+slot][sink]=available[slot];
        long flow=0;
        while(flow<required){
            int[] parent=new int[n];java.util.Arrays.fill(parent,-1);parent[source]=source;
            int[] queue=new int[n];int head=0,tail=0;queue[tail++]=source;
            while(head<tail&&parent[sink]<0){int from=queue[head++];for(int to=0;to<n;to++)if(parent[to]<0&&residual[from][to]>0){parent[to]=from;queue[tail++]=to;}}
            if(parent[sink]<0)return null;
            long amount=required-flow;
            for(int to=sink;to!=source;to=parent[to])amount=Math.min(amount,residual[parent[to]][to]);
            for(int to=sink;to!=source;to=parent[to]){residual[parent[to]][to]-=amount;residual[to][parent[to]]+=amount;}
            flow+=amount;
        }
        int[] consumed=new int[available.length];
        for(int slot=0;slot<available.length;slot++)consumed[slot]=(int)(available[slot]-residual[firstSlot+slot][sink]);
        return consumed;
    }
}
