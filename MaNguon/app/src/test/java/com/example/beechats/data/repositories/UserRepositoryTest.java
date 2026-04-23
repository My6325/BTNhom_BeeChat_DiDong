package com.example.beechats.data.repositories;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.List;

/**
 * Unit tests cho UserRepository.
 * Mock Firestore chain để test không cần device/emulator.
 */
public class UserRepositoryTest {

    @Mock
    private FirebaseFirestore mockFirestore;

    @Mock
    private CollectionReference mockCollection;

    @Mock
    private DocumentReference mockDocument;

    @Mock
    private UserRepository.OnCompleteCallback mockCallback;

    private UserRepository repository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        // Stub Firestore chain: db.collection(...).document(...) → mockDocument
        when(mockFirestore.collection(anyString())).thenReturn(mockCollection);
        when(mockCollection.document(anyString())).thenReturn(mockDocument);
        repository = new UserRepository(mockFirestore);
    }

    // -----------------------------------------------------------------------
    // TC19: updateProfile — Happy Path: displayName hợp lệ → Firestore update → onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void updateProfile_validInput_callsFirestoreUpdateAndOnSuccess() {
        Task<Void> updateTask = buildVoidSuccessTask();
        when(mockDocument.update(anyMap())).thenReturn(updateTask);

        repository.updateProfile("uid-001", "Bee", "Bio test", mockCallback);

        verify(mockDocument).update(anyMap());
        verify(mockCallback).onSuccess();
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC20: updateProfile — displayName rỗng → onError(), Firestore không gọi
    // -----------------------------------------------------------------------

    @Test
    public void updateProfile_emptyDisplayName_callsOnErrorWithoutFirestore() {
        repository.updateProfile("uid-001", "", "Bio test", mockCallback);

        verify(mockCallback).onError("Tên hiển thị không được để trống.");
        verify(mockCallback, never()).onSuccess();
        verify(mockDocument, never()).update(anyMap());
    }

    // -----------------------------------------------------------------------
    // TC21: updateProfile — displayName null → onError(), Firestore không gọi
    // -----------------------------------------------------------------------

    @Test
    public void updateProfile_nullDisplayName_callsOnErrorWithoutFirestore() {
        repository.updateProfile("uid-001", null, "Bio test", mockCallback);

        verify(mockCallback).onError("Tên hiển thị không được để trống.");
        verify(mockCallback, never()).onSuccess();
        verify(mockDocument, never()).update(anyMap());
    }

    // -----------------------------------------------------------------------
    // TC22: updateProfile — Firestore update fail → onError(errorMessage)
    // -----------------------------------------------------------------------

    @Test
    public void updateProfile_firestoreFails_callsOnErrorWithMessage() {
        Exception exception = new Exception("Firestore connection error");
        Task<Void> updateTask = buildVoidFailureTask(exception);
        when(mockDocument.update(anyMap())).thenReturn(updateTask);

        repository.updateProfile("uid-001", "Bee", "Bio test", mockCallback);

        verify(mockCallback).onError("Firestore connection error");
        verify(mockCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC23: generateSearchKeywords — "Bee" → chứa ["b", "be", "bee"]
    // -----------------------------------------------------------------------

    @Test
    public void generateSearchKeywords_singleWord_containsAllPrefixes() {
        List<String> keywords = UserRepository.generateSearchKeywords("Bee");

        assertTrue(keywords.contains("b"));
        assertTrue(keywords.contains("be"));
        assertTrue(keywords.contains("bee"));
        assertEquals(3, keywords.size());
    }

    // -----------------------------------------------------------------------
    // TC24: generateSearchKeywords — displayName null → trả về list rỗng
    // -----------------------------------------------------------------------

    @Test
    public void generateSearchKeywords_nullInput_returnsEmptyList() {
        List<String> keywords = UserRepository.generateSearchKeywords(null);

        assertTrue(keywords.isEmpty());
    }

    // -----------------------------------------------------------------------
    // TC25: generateSearchKeywords — tên tiếng Việt có dấu → bỏ dấu + prefix đúng
    // -----------------------------------------------------------------------

    @Test
    public void generateSearchKeywords_vietnameseName_stripsDiacriticsAndGeneratesPrefixes() {
        List<String> keywords = UserRepository.generateSearchKeywords("Nguyễn");

        // Sau khi bỏ dấu: "nguyen" → prefix: n, ng, ngu, nguy, nguye, nguyen
        assertTrue(keywords.contains("n"));
        assertTrue(keywords.contains("ng"));
        assertTrue(keywords.contains("nguyen"));
    }

    // -----------------------------------------------------------------------
    // Helper: tạo mock Task<Void> thành công
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<Void> buildVoidSuccessTask() {
        Task<Void> mockTask = mock(Task.class);
        doAnswer(inv -> {
            ((OnSuccessListener<Void>) inv.getArgument(0)).onSuccess(null);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockTask);
        return mockTask;
    }

    // -----------------------------------------------------------------------
    // Helper: tạo mock Task<Void> thất bại
    // -----------------------------------------------------------------------

    @SuppressWarnings("unchecked")
    private Task<Void> buildVoidFailureTask(Exception exception) {
        Task<Void> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTask);
        doAnswer(inv -> {
            ((OnFailureListener) inv.getArgument(0)).onFailure(exception);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
        return mockTask;
    }
}
