package com.siemens.internship;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ServiceTests {

    @Mock
    private ItemRepository itemRepository;

    @InjectMocks
    private ItemService itemService;

    @Test
    void testFindAll() {
        List<Item> items = List.of(new Item(1L, "Test1", "descrip", "NEW", "a@b.com"));
        when(itemRepository.findAll()).thenReturn(items);

        List<Item> result = itemService.findAll();

        assertEquals(1, result.size());
        assertEquals("Test1", result.get(0).getName());
    }

    @Test
    void testFindById_found() {
        Item item = new Item(2L, "Nume", "descr", "NEW", "x@y.com");
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item));

        Optional<Item> result = itemService.findById(2L);

        assertTrue(result.isPresent());
        assertEquals("Nume", result.get().getName());
    }

    @Test
    void testFindById_notFound() {
        when(itemRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<Item> result = itemService.findById(999L);

        assertFalse(result.isPresent());
    }

    @Test
    void testSave() {
        Item item = new Item(null, "SaveT", "desc", "NEW", "valid@gmail.com");
        when(itemRepository.save(item)).thenReturn(item);

        Item saved = itemService.save(item);

        assertEquals("SaveT", saved.getName());
    }

    @Test
    void testDeleteById() {
        doNothing().when(itemRepository).deleteById(5L);

        itemService.deleteById(5L);

        verify(itemRepository, times(1)).deleteById(5L);
    }

    @Test
    void testProcessItemsAsync_logic() throws Exception {

        List<Long> ids = List.of(1L, 2L);
        Item item1 = new Item(1L, "Unu", "desc", "NEW", "unu@mail.com");
        Item item2 = new Item(2L, "Doi", "desc", "NEW", "doi@mail.com");

        when(itemRepository.findAllIds()).thenReturn(ids);
        when(itemRepository.findById(1L)).thenReturn(Optional.of(item1));
        when(itemRepository.findById(2L)).thenReturn(Optional.of(item2));
        when(itemRepository.save(any(Item.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CompletableFuture<List<Item>> future = itemService.processItemsAsync();
        List<Item> result = future.get();

        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(item -> "PROCESSED".equals(item.getStatus())));
    }
}
