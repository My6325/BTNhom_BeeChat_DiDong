package com.example.beechats.data.repositories;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.beechats.data.models.User;
import com.example.beechats.data.models.UserSettings;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests cho SettingsRepository (TC189 – TC202).
 * Mock Firestore chain để test không cần device/emulator.
 */
public class SettingsRepositoryTest {

    @Mock private FirebaseFirestore mockFirestore;
    @Mock private CollectionReference mockUsersCollection;
    @Mock private DocumentReference mockUserDoc;
    @Mock private DocumentSnapshot mockSnapshot;

    @Mock private SettingsRepository.OnCompleteCallback mockCompleteCallback;
    @Mock private SettingsRepository.OnSettingsCallback mockSettingsCallback;
    @Mock private SettingsRepository.OnBooleanCallback mockBooleanCallback;

    private SettingsRepository settingsRepository;

    @Before
    public void setUp() {
        MockitoAnnotations.openMocks(this);
        settingsRepository = new SettingsRepository(mockFirestore);
        when(mockFirestore.collection("users")).thenReturn(mockUsersCollection);
        when(mockUsersCollection.document(anyString())).thenReturn(mockUserDoc);
    }

    // ============================================================
    // TC189–TC193: updatePrivacySettings
    // ============================================================

    /** TC189: userId hợp lệ, tắt cả hai → onSuccess. */
    @Test
    public void TC189_updatePrivacySettings_bothFalse_callsOnSuccess() {
        Task<Void> task = buildVoidSuccessTask();
        when(mockUserDoc.update(anyMap())).thenReturn(task);

        settingsRepository.updatePrivacySettings("uid1", false, false, mockCompleteCallback);

        verify(mockCompleteCallback).onSuccess();
        verify(mockCompleteCallback, never()).onError(anyString());
    }

    /** TC190: userId null → onError ngay, không gọi Firestore. */
    @Test
    public void TC190_updatePrivacySettings_nullUserId_callsOnError() {
        settingsRepository.updatePrivacySettings(null, true, true, mockCompleteCallback);

        verify(mockCompleteCallback).onError(anyString());
        verify(mockUserDoc, never()).update(anyMap());
    }

    /** TC191: userId rỗng → onError ngay, không gọi Firestore. */
    @Test
    public void TC191_updatePrivacySettings_emptyUserId_callsOnError() {
        settingsRepository.updatePrivacySettings("   ", true, true, mockCompleteCallback);

        verify(mockCompleteCallback).onError(anyString());
        verify(mockUserDoc, never()).update(anyMap());
    }

    /** TC192: bật cả hai → onSuccess. */
    @Test
    public void TC192_updatePrivacySettings_bothTrue_callsOnSuccess() {
        Task<Void> task = buildVoidSuccessTask();
        when(mockUserDoc.update(anyMap())).thenReturn(task);

        settingsRepository.updatePrivacySettings("uid1", true, true, mockCompleteCallback);

        verify(mockCompleteCallback).onSuccess();
        verify(mockCompleteCallback, never()).onError(anyString());
    }

    /** TC193: Firestore thất bại → onError. */
    @Test
    public void TC193_updatePrivacySettings_firestoreFailure_callsOnError() {
        Task<Void> task = buildVoidFailureTask(new Exception("Network error"));
        when(mockUserDoc.update(anyMap())).thenReturn(task);

        settingsRepository.updatePrivacySettings("uid1", false, false, mockCompleteCallback);

        verify(mockCompleteCallback).onError("Network error");
        verify(mockCompleteCallback, never()).onSuccess();
    }

    // ============================================================
    // TC194–TC197: getPrivacySettings
    // ============================================================

    /** TC194: snapshot tồn tại, settings có isOnlineVisible=true → onSuccess. */
    @Test
    public void TC194_getPrivacySettings_visible_callsOnSuccess() {
        UserSettings settings = new UserSettings();
        User user = new User();
        user.setSettings(settings);

        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.toObject(User.class)).thenReturn(user);
        Task<DocumentSnapshot> task = buildDocSuccessTask(mockSnapshot);
        when(mockUserDoc.get()).thenReturn(task);

        settingsRepository.getPrivacySettings("uid1", mockSettingsCallback);

        verify(mockSettingsCallback).onSuccess(settings);
        verify(mockSettingsCallback, never()).onError(anyString());
    }

    /** TC195: isOnlineVisible=false → onSuccess với settings phản ánh đúng. */
    @Test
    public void TC195_getPrivacySettings_hidden_callsOnSuccessWithFalse() {
        UserSettings settings = new UserSettings();
        settings.setOnlineVisible(false);
        User user = new User();
        user.setSettings(settings);

        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.toObject(User.class)).thenReturn(user);
        Task<DocumentSnapshot> task = buildDocSuccessTask(mockSnapshot);
        when(mockUserDoc.get()).thenReturn(task);

        settingsRepository.getPrivacySettings("uid1", mockSettingsCallback);

        verify(mockSettingsCallback).onSuccess(settings);
    }

    /** TC196: snapshot không tồn tại → onError. */
    @Test
    public void TC196_getPrivacySettings_snapshotNotExist_callsOnError() {
        when(mockSnapshot.exists()).thenReturn(false);
        Task<DocumentSnapshot> task = buildDocSuccessTask(mockSnapshot);
        when(mockUserDoc.get()).thenReturn(task);

        settingsRepository.getPrivacySettings("uid1", mockSettingsCallback);

        verify(mockSettingsCallback).onError(anyString());
        verify(mockSettingsCallback, never()).onSuccess(any());
    }

    /** TC197: Firestore thất bại → onError. */
    @Test
    public void TC197_getPrivacySettings_firestoreFailure_callsOnError() {
        Task<DocumentSnapshot> task = buildDocFailureTask(new Exception("Timeout"));
        when(mockUserDoc.get()).thenReturn(task);

        settingsRepository.getPrivacySettings("uid1", mockSettingsCallback);

        verify(mockSettingsCallback).onError("Timeout");
        verify(mockSettingsCallback, never()).onSuccess(any());
    }

    // ============================================================
    // TC198–TC202: getEffectiveOnlineStatus
    // ============================================================

    /** TC198: isOnline=true, isOnlineVisible=true → onResult(true). */
    @Test
    public void TC198_getEffectiveOnlineStatus_onlineVisible_returnsTrue() {
        UserSettings settings = new UserSettings();
        User user = new User();
        user.setOnline(true);
        user.setSettings(settings);

        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.toObject(User.class)).thenReturn(user);
        Task<DocumentSnapshot> task = buildDocSuccessTask(mockSnapshot);
        when(mockUserDoc.get()).thenReturn(task);

        settingsRepository.getEffectiveOnlineStatus("uid1", mockBooleanCallback);

        verify(mockBooleanCallback).onResult(true);
        verify(mockBooleanCallback, never()).onError(anyString());
    }

    /** TC199: isOnline=true nhưng isOnlineVisible=false → onResult(false). */
    @Test
    public void TC199_getEffectiveOnlineStatus_onlineButHidden_returnsFalse() {
        UserSettings settings = new UserSettings();
        settings.setOnlineVisible(false);
        User user = new User();
        user.setOnline(true);
        user.setSettings(settings);

        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.toObject(User.class)).thenReturn(user);
        Task<DocumentSnapshot> task = buildDocSuccessTask(mockSnapshot);
        when(mockUserDoc.get()).thenReturn(task);

        settingsRepository.getEffectiveOnlineStatus("uid1", mockBooleanCallback);

        verify(mockBooleanCallback).onResult(false);
    }

    /** TC200: isOnline=false, isOnlineVisible=true → onResult(false). */
    @Test
    public void TC200_getEffectiveOnlineStatus_offlineVisible_returnsFalse() {
        UserSettings settings = new UserSettings();
        User user = new User();
        user.setOnline(false);
        user.setSettings(settings);

        when(mockSnapshot.exists()).thenReturn(true);
        when(mockSnapshot.toObject(User.class)).thenReturn(user);
        Task<DocumentSnapshot> task = buildDocSuccessTask(mockSnapshot);
        when(mockUserDoc.get()).thenReturn(task);

        settingsRepository.getEffectiveOnlineStatus("uid1", mockBooleanCallback);

        verify(mockBooleanCallback).onResult(false);
    }

    /** TC201: Firestore thất bại → onError. */
    @Test
    public void TC201_getEffectiveOnlineStatus_firestoreFailure_callsOnError() {
        Task<DocumentSnapshot> task = buildDocFailureTask(new Exception("Auth error"));
        when(mockUserDoc.get()).thenReturn(task);

        settingsRepository.getEffectiveOnlineStatus("uid1", mockBooleanCallback);

        verify(mockBooleanCallback).onError("Auth error");
        verify(mockBooleanCallback, never()).onResult(any(Boolean.class));
    }

    /** TC202: userId null → onError ngay, không gọi Firestore. */
    @Test
    public void TC202_getEffectiveOnlineStatus_nullUserId_callsOnError() {
        settingsRepository.getEffectiveOnlineStatus(null, mockBooleanCallback);

        verify(mockBooleanCallback).onError(anyString());
        verify(mockUserDoc, never()).get();
    }

    // ============================================================
    // Helpers: tạo Task giả lập Firestore (dùng doAnswer pattern)
    // ============================================================

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

    @SuppressWarnings("unchecked")
    private Task<DocumentSnapshot> buildDocSuccessTask(DocumentSnapshot snapshot) {
        Task<DocumentSnapshot> mockTask = mock(Task.class);
        doAnswer(inv -> {
            ((OnSuccessListener<DocumentSnapshot>) inv.getArgument(0)).onSuccess(snapshot);
            return mockTask;
        }).when(mockTask).addOnSuccessListener(any(OnSuccessListener.class));
        when(mockTask.addOnFailureListener(any(OnFailureListener.class))).thenReturn(mockTask);
        return mockTask;
    }

    @SuppressWarnings("unchecked")
    private Task<DocumentSnapshot> buildDocFailureTask(Exception exception) {
        Task<DocumentSnapshot> mockTask = mock(Task.class);
        when(mockTask.addOnSuccessListener(any(OnSuccessListener.class))).thenReturn(mockTask);
        doAnswer(inv -> {
            ((OnFailureListener) inv.getArgument(0)).onFailure(exception);
            return mockTask;
        }).when(mockTask).addOnFailureListener(any(OnFailureListener.class));
        return mockTask;
    }
}
