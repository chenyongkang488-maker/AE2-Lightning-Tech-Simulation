package dev.overloadsim.compat;
/** Two independent voltage keys, with a durable refund debt on partial extraction. */
public final class SimulationLightningPayment {
    public interface Storage {
        long extract(boolean extreme,long amount,boolean simulate);
        long insert(boolean extreme,long amount);
    }
    public record Result(boolean paid,long refundHv,long refundEhv){}
    public static Result pay(Storage storage,long hv,long ehv){
        if(hv<0||ehv<0)throw new IllegalArgumentException("negative fee");
        if(storage.extract(false,hv,true)!=hv||storage.extract(true,ehv,true)!=ehv)return new Result(false,0,0);
        long gotHv=storage.extract(false,hv,false);
        if(gotHv!=hv)return new Result(false,gotHv-storage.insert(false,gotHv),0);
        long gotEhv=storage.extract(true,ehv,false);
        if(gotEhv!=ehv)return new Result(false,gotHv-storage.insert(false,gotHv),gotEhv-storage.insert(true,gotEhv));
        return new Result(true,0,0);
    }
}
