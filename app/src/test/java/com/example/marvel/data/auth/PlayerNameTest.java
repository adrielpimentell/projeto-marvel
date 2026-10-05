package com.example.marvel.data.auth;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class PlayerNameTest {

    @Test
    public void acceptsLettersNumbersAndUnderscoreFrom3To20() {
        assertTrue(PlayerName.isValid("tony_99"));
        assertTrue(PlayerName.isValid("Abc"));
        assertTrue(PlayerName.isValid("a_b_c_d_e_f_g_h_i_jk"));
    }

    @Test
    public void rejectsWrongSizeOrCharacters() {
        assertFalse(PlayerName.isValid("ab"));
        assertFalse(PlayerName.isValid("a_b_c_d_e_f_g_h_i_jkl"));
        assertFalse(PlayerName.isValid("tony 99"));
        assertFalse(PlayerName.isValid("tony-99"));
        assertFalse(PlayerName.isValid("joão"));
        assertFalse(PlayerName.isValid(""));
        assertFalse(PlayerName.isValid(null));
    }

    @Test
    public void keyIgnoresCaseAndOuterSpaces() {
        assertEquals("tony_99", PlayerName.key("  Tony_99 "));
        assertEquals(PlayerName.key("TONY_99"), PlayerName.key("tony_99"));
    }
}
