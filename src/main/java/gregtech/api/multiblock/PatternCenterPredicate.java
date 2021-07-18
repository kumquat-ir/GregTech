package gregtech.api.multiblock;

import java.util.function.Predicate;

public interface PatternCenterPredicate extends Predicate<IBlockWorldState> {

    static PatternCenterPredicate from(Predicate<IBlockWorldState> predicate) {
        return predicate::test;
    }
}
