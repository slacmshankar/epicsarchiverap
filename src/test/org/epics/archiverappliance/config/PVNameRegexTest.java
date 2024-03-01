package org.epics.archiverappliance.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.List;

/**
 * Small unit test to make sure the regex that checks for valid pv names does something reasonable
 * @author mshankar
 *
 */
public class PVNameRegexTest {

    @Test
    public void testValidPVNames() {

        List<String> pvNames = List.of(
                "archappl:arch:sine",
                "archappl:arch:sine1",
                "archappl:arch:sine1.HIHI",
                "archappl:arch:sine100",
                "archappl_arch_sine",
                "archappl-arch-sine",
                "archappl+arch+sine",
                "archappl[arch]sine",
                "archappl<arch>sine",
                "archappl:arch;sine",
                "archappl:ar/h:sine",
                "P#6:000357",
                "LN-AM{RadMon:1}Lvl:Raw-I",
                "TBGR001E01.{'dbnd':{'abs':1}}",
                "archappl:ar'h:sine",
                "CrS-PHS:Cryo-D-0100:AmbientTemp.{'dbnd':{'rel':0.1}}",
                "XDCM001I01.{flv('H-GX')}");
        for (String pvName : pvNames) {
            Assertions.assertTrue(PVNames.isValidPVName(pvName), "Valid pvName is deemed invalid " + pvName);
            Assertions.assertEquals(pvName, PVNames.normalizePVName(pvName));
        }
    }

    @Test
    public void testNormalizePVName() {
        String pvName = "CrS-PHS:Cryo-D-0100:AmbientTemp.VAL";
        Assertions.assertEquals("CrS-PHS:Cryo-D-0100:AmbientTemp", PVNames.normalizePVName(pvName));
    }

    @Test
    public void testInvalidPVNames() {
        List<String> pvNames = List.of(
                "\"archappl:arch:sine",
                "archappl:arch:sine\"",
                "archappl:ar!h:sine",
                "archappl:ar$h:sine",
                "archappl:ar%h:sine",
                "archappl:ar*h:sine",
                "archappl:ar\\h:sine",
                "archappl:ar|h:sine",
                "archappl:ar?h:sine",
                "archappl:ar&h:sine",
                "archappl:ar@ch:sine",
                "archappl:(arch):sine",
                "archappl:arch:sine=1.0",
                "");
        for (String pvName : pvNames) {
            Assertions.assertFalse(PVNames.isValidPVName(pvName), "Invalid pvName is deemed valid " + pvName);
        }
        Assertions.assertFalse(PVNames.isValidPVName(null), "null is deemed invalid");
    }
}
