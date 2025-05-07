package com.siemens.internship;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class InternshipApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ItemService itemService;

    @Test
    void testGetAllItems() throws Exception {
        List<Item> items = List.of(new Item(1L, "Test", "description", "NEW", "email@ex.com"));
        when(itemService.findAll()).thenReturn(items);

        mockMvc.perform(get("/api/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].name").value("Test"));
    }

    @Test
    void testGetItemById_found() throws Exception {
        Item item = new Item(2L, "Item2", "desccription", "NEW", "user@ex.com");
        when(itemService.findById(2L)).thenReturn(Optional.of(item));

        mockMvc.perform(get("/api/items/2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Item2"));
    }

    @Test
    void testGetItemById_notFound() throws Exception {
        when(itemService.findById(9999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/items/9999"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testCreateItem_valid() throws Exception {
        Item item = new Item(null, "NewIteM", "desc", "NEW", "valid@gmail.com");
        when(itemService.save(any(Item.class))).thenReturn(item);

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(item)))
                .andExpect(status().isCreated());
    }

    @Test
    void testCreateItem_invalidEmail() throws Exception {
        Item invalidItem = new Item(null, "ErrEmail", "desc", "NEW", "inv-email");

        mockMvc.perform(post("/api/items")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidItem)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void testUpdateItem_exists() throws Exception {
        Item existing = new Item(5L, "OldVal", "desc", "NEW", "old@gmail.com");
        Item updated = new Item(5L, "Updated", "desc", "NEW", "old@email.com");

        when(itemService.findById(5L)).thenReturn(Optional.of(existing));
        when(itemService.save(any(Item.class))).thenReturn(updated);

        mockMvc.perform(put("/api/items/5")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Updated"));
    }

    @Test
    void testUpdateItem_notFound() throws Exception {
        when(itemService.findById(10L)).thenReturn(Optional.empty());

        mockMvc.perform(put("/api/items/10")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new Item())))
                .andExpect(status().isNotFound());
    }

    @Test
    void testDeleteItem_exists() throws Exception {
        when(itemService.findById(1L)).thenReturn(Optional.of(new Item()));
        doNothing().when(itemService).deleteById(1L);

        mockMvc.perform(delete("/api/items/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteItem_notFound() throws Exception {
        when(itemService.findById(404L)).thenReturn(Optional.empty());

        mockMvc.perform(delete("/api/items/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testProcessItemsAsync() throws Exception {
        List<Item> processed = List.of(new Item(1L, "Processed", "done", "PROCESSED", "a@b.com"));
        when(itemService.processItemsAsync()).thenReturn(CompletableFuture.completedFuture(processed));

        MvcResult mvcResult = mockMvc.perform(get("/api/items/process"))
                .andExpect(request().asyncStarted())
                .andReturn();

        mockMvc.perform(asyncDispatch(mvcResult))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].status").value("PROCESSED"));
    }


}
