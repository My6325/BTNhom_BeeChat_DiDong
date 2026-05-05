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
import org.mockito.ArgumentCaptor;
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
    // TC54: updateOnlineStatus — isOnline=true + Firestore success → onSuccess() được gọi
    // -----------------------------------------------------------------------

    @Test
    public void updateOnlineStatus_onlineTrue_firestoreSuccess_callsOnSuccess() {
        Task<Void> updateTask = buildVoidSuccessTask();
        when(mockDocument.update(anyMap())).thenReturn(updateTask);

        repository.updateOnlineStatus("uid-001", true, mockCallback);

        verify(mockDocument).update(anyMap());
        verify(mockCallback).onSuccess();
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC55: updateOnlineStatus — isOnline=false + Firestore success → onSuccess() được gọi
    // -----------------------------------------------------------------------

    @Test
    public void updateOnlineStatus_onlineFalse_firestoreSuccess_callsOnSuccess() {
        Task<Void> updateTask = buildVoidSuccessTask();
        when(mockDocument.update(anyMap())).thenReturn(updateTask);

        repository.updateOnlineStatus("uid-001", false, mockCallback);

        verify(mockDocument).update(anyMap());
        verify(mockCallback).onSuccess();
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC56: updateOnlineStatus — Firestore fail → onError(errorMessage) được gọi
    // -----------------------------------------------------------------------

    @Test
    public void updateOnlineStatus_firestoreFails_callsOnErrorWithMessage() {
        Exception exception = new Exception("Network timeout");
        Task<Void> updateTask = buildVoidFailureTask(exception);
        when(mockDocument.update(anyMap())).thenReturn(updateTask);

        repository.updateOnlineStatus("uid-001", true, mockCallback);

        verify(mockCallback).onError("Network timeout");
        verify(mockCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC83: updateFcmToken — userId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void updateFcmToken_nullUserId_callsOnError() {
        repository.updateFcmToken(null, "token-xyz", mockCallback);

        verify(mockCallback).onError("ID người dùng không hợp lệ.");
        verify(mockCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC84: updateFcmToken — token null/rỗng → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void updateFcmToken_nullToken_callsOnError() {
        repository.updateFcmToken("uid-001", null, mockCallback);

        verify(mockCallback).onError("FCM Token không hợp lệ.");
        verify(mockCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    @Test
    public void updateFcmToken_emptyToken_callsOnError() {
        repository.updateFcmToken("uid-001", "", mockCallback);

        verify(mockCallback).onError("FCM Token không hợp lệ.");
        verify(mockCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC85: updateFcmToken — params hợp lệ → Firestore update("fcmToken",...) → onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void updateFcmToken_validParams_callsFirestoreUpdateAndOnSuccess() {
        Task<Void> updateTask = buildVoidSuccessTask();
        when(mockDocument.update(anyString(), any(Object.class))).thenReturn(updateTask);

        ArgumentCaptor<String> fieldCaptor = ArgumentCaptor.forClass(String.class);

        repository.updateFcmToken("uid-001", "token-xyz-123", mockCallback);

        verify(mockDocument).update(fieldCaptor.capture(), any(Object.class));
        assertEquals("fcmToken", fieldCaptor.getValue());
        verify(mockCallback).onSuccess();
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC86: updateFcmToken — Firestore fails → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void updateFcmToken_firestoreFails_callsOnError() {
        Exception exception = new Exception("Network error");
        Task<Void> updateTask = buildVoidFailureTask(exception);
        when(mockDocument.update(anyString(), any(Object.class))).thenReturn(updateTask);

        repository.updateFcmToken("uid-001", "token-xyz-123", mockCallback);

        verify(mockCallback).onError("Network error");
        verify(mockCallback, never()).onSuccess();
    }

    // -----------------------------------------------------------------------
    // TC87: clearFcmToken — userId null → onError ngay
    // -----------------------------------------------------------------------

    @Test
    public void clearFcmToken_nullUserId_callsOnError() {
        repository.clearFcmToken(null, mockCallback);

        verify(mockCallback).onError("ID người dùng không hợp lệ.");
        verify(mockCallback, never()).onSuccess();
        verify(mockFirestore, never()).collection(anyString());
    }

    // -----------------------------------------------------------------------
    // TC88: clearFcmToken — userId hợp lệ → update("fcmToken", "") → onSuccess()
    // -----------------------------------------------------------------------

    @Test
    public void clearFcmToken_validUserId_callsFirestoreUpdateWithEmptyAndOnSuccess() {
        Task<Void> updateTask = buildVoidSuccessTask();
        when(mockDocument.update(anyString(), any(Object.class))).thenReturn(updateTask);

        ArgumentCaptor<String> fieldCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Object> valueCaptor = ArgumentCaptor.forClass(Object.class);

        repository.clearFcmToken("uid-001", mockCallback);

        verify(mockDocument).update(fieldCaptor.capture(), valueCaptor.capture());
        assertEquals("fcmToken", fieldCaptor.getValue());
        assertEquals("", valueCaptor.getValue());
        verify(mockCallback).onSuccess();
        verify(mockCallback, never()).onError(anyString());
    }

    // -----------------------------------------------------------------------
    // TC89: clearFcmToken — Firestore fails → onError(message)
    // -----------------------------------------------------------------------

    @Test
    public void clearFcmToken_firestoreFails_callsOnError() {
        Exception exception = new Exception("Network error");
        Task<Void> updateTask = buildVoidFailureTask(exception);
        when(mockDocument.update(anyString(), any(Object.class))).thenReturn(updateTask);

        repository.clearFcmToken("uid-001", mockCallback);

        verify(mockCallback).onError("Network error");
        verify(mockCallback, never()).onSuccess();
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
