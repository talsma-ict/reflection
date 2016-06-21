package nl.talsmasoftware.reflection.dto;

import java.math.BigDecimal;

/**
 * Simple datastructure representation providing only public fields.
 *
 * @author <a href="mailto:info@talsma-software.nl">Sjoerd Talsma</a>
 */
public class DtoRepresentationV1 extends AbstractDto {

    public long number;
    public String name;
    public BigDecimal amountInEuros;
    public DtoRepresentationV1 subObject;

}
