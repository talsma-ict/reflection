package nl.talsmasoftware.reflection.dto;

import java.math.BigDecimal;

/**
 * Simple datastructure representation providing only public fields.
 *
 * @author Sjoerd Talsma
 */
public class DtoRepresentationV1 extends AbstractDto {

    public long number;
    public String name;
    public BigDecimal amountInEuros;
    public DtoRepresentationV1 subObject;

}
