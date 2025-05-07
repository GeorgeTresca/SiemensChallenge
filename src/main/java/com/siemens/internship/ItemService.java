package com.siemens.internship;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.*;

@Service
public class ItemService {
    @Autowired
    private ItemRepository itemRepository;
    private static ExecutorService executor = Executors.newFixedThreadPool(10);
    private List<Item> processedItems = new ArrayList<>();
    private int processedCount = 0;


    public List<Item> findAll() {
        return itemRepository.findAll();
    }

    public Optional<Item> findById(Long id) {
        return itemRepository.findById(id);
    }

    public Item save(Item item) {
        return itemRepository.save(item);
    }

    public void deleteById(Long id) {
        itemRepository.deleteById(id);
    }


    /**
     * Your Tasks
     * Identify all concurrency and asynchronous programming issues in the code
     * Fix the implementation to ensure:
     * All items are properly processed before the CompletableFuture completes
     * Thread safety for all shared state
     * Proper error handling and propagation
     * Efficient use of system resources
     * Correct use of Spring's @Async annotation
     * Add appropriate comments explaining your changes and why they fix the issues
     * Write a brief explanation of what was wrong with the original implementation
     *
     * Hints
     * Consider how CompletableFuture composition can help coordinate multiple async operations
     * Think about appropriate thread-safe collections
     * Examine how errors are handled and propagated
     * Consider the interaction between Spring's @Async and CompletableFuture
     */

    @Async
    public CompletableFuture<List<Item>> processItemsAsync() {
        List<Long> itemIds = itemRepository.findAllIds();

        // The future list will hold all the asynchronous processing tasks

        List<CompletableFuture<Item>> futures = new ArrayList<>();

        // I used a thread-safe collection for storing the successfully processed items
        // CopyOnWriteArrayList avoids concurrency issues when multiple threads acces it for writing

        List<Item> result = new CopyOnWriteArrayList<>();

        //For each item, a separate asynchronous task is submitted

        for (Long id : itemIds) {
            CompletableFuture<Item> future = CompletableFuture.supplyAsync(() -> {
                try {
                    Thread.sleep(100);

                    //Item retrieval from the database

                    Optional<Item> optionalItem = itemRepository.findById(id);
                    if (optionalItem.isEmpty()) return null;  //If the item is not found, processing is skipped

                    // After a succesful retrieval, the item's status is updated to "PROCESSED"

                    Item item = optionalItem.get();
                    item.setStatus("PROCESSED");
                    return itemRepository.save(item);
                } catch (Exception e) {
                    //The errors are handled for each individual item without crashing the overall process

                    System.err.println("Caught error while processing item with id " + id + ": " + e.getMessage());
                    return null;
                }

                //The successfully processed item is added to the result list
            }, executor).thenApply(processedItem -> {
                if (processedItem != null) result.add(processedItem);
                return processedItem;
            });

            futures.add(future);
        }

        //All the asynchronous task are waited until they are completed
        // CompletableFuture.allOf ensures the main future completes only when all the futures used for items are available
        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> result);
    }


/**
 * Issues with the original implementation:
 *
 *    - Although annotated with @Async, the method returned a List<Item>
 *    - Background tasks were launched using runAsync() but their completion was never waited for
 *    - As a result, it returned an incomplete or empty list before processing finished
 *    - Tasks ran independently without a way to detect when all had completed
 *    - There was no mechanism for synchronizing futures
 *    - It used a non-thread-safe ArrayList (processedItems) and a shared counter (processedCount).
 *    - They were accessed concurrently from multiple threads without synchronization, favouring incorrenct processing
 *    - Only InterruptedException was caught, ignoring other runtime failures

 */
//
//    @Async
//    public List<Item> processItemsAsync() {
//
//        List<Long> itemIds = itemRepository.findAllIds();
//
//        for (Long id : itemIds) {
//            CompletableFuture.runAsync(() -> {
//                try {
//                    Thread.sleep(100);
//
//                    Item item = itemRepository.findById(id).orElse(null);
//                    if (item == null) {
//                        return;
//                    }
//
//                    processedCount++;
//
//                    item.setStatus("PROCESSED");
//                    itemRepository.save(item);
//                    processedItems.add(item);
//
//                } catch (InterruptedException e) {
//                    System.out.println("Error: " + e.getMessage());
//                }
//            }, executor);
//        }
//
//        return processedItems;
//    }

}

