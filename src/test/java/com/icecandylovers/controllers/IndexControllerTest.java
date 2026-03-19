package com.icecandylovers.controllers;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class IndexControllerTest {

    @Test
    void indexPage_deveRetornarViewIndex() {
        IndexController controller = new IndexController();
        String view = controller.indexPage();
        assertEquals("index", view);
    }
}
