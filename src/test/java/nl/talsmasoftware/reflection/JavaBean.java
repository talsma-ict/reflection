package nl.talsmasoftware.reflection;

/**
 */
public class JavaBean {
    private String value1;
    private Integer value2;
    private Boolean value3;
    private Object value4;

    JavaBean(String value1, Integer value2, Boolean value3, Object value4) {
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
        this.value4 = value4;
    }

    public String getValue1() {
        return value1;
    }

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    public Integer getValue2() {
        return value2;
    }

    public void setValue2(Integer value2) {
        this.value2 = value2;
    }


    public Boolean getValue3() {
        return value3;
    }

    public void setValue3(Boolean value3) {
        this.value3 = value3;
    }

    public Object getValue4() {
        return value4;
    }

    public void setValue4(Object value4) {
        this.value4 = value4;
    }

}
