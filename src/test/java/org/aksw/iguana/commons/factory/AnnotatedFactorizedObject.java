package org.aksw.iguana.commons.factory;

import org.aksw.iguana.commons.annotation.Nullable;
import org.aksw.iguana.commons.annotation.ParameterNames;
import org.aksw.iguana.commons.annotation.Shorthand;

@Shorthand(value = "facto")
public class AnnotatedFactorizedObject extends FactorizedObject {
    public AnnotatedFactorizedObject(String[] args, String[] args2) {
        this.setArgs(args);
        this.setArgs2(args2);
    }

    @ParameterNames(names={"a","b","c"})
    public AnnotatedFactorizedObject(String a, String b, String c) {
        this.setArgs(new String[] {a, b, c});
    }

    @ParameterNames(names={"a","b"})
    public AnnotatedFactorizedObject(String a, @Nullable String b) {
        this.setArgs(new String[] {a, b==null?"wasNull":b});
    }

    public AnnotatedFactorizedObject() {
        args = new String[] {"a3", "b3"};
    }

}
