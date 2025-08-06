package com.example.reservely.di

import android.content.Context
import com.example.reservely.data.remote.CloudinaryApi
import com.example.reservely.BuildConfig
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.functions.FirebaseFunctions
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.getstream.chat.android.client.ChatClient
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object FirebaseModule {

    @Provides
    fun provideFirestore(): FirebaseFirestore = FirebaseFirestore.getInstance()

    @Provides
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    fun provideFunctions(): FirebaseFunctions =
        FirebaseFunctions.getInstance("us-central1")

    @Provides
    fun provideFusedLocationClient(@ApplicationContext context: Context): FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    @Provides
    @IoDispatcher
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

}


@Module
@InstallIn(SingletonComponent::class)
object CloudinaryModule {

    private const val CLOUD_NAME = BuildConfig.CLOUDINARY_CLOUD_NAME
    private const val UPLOAD_PRESET = BuildConfig.CLOUDINARY_UPLOAD_PRESET

    @Provides
    @Singleton
    fun provideCloudinaryConfig(): CloudinaryConfig = CloudinaryConfig(
        cloudName = CLOUD_NAME,
        uploadPreset = UPLOAD_PRESET
    )

    @Provides
    @Singleton
    fun provideCloudinaryApi(config: CloudinaryConfig): CloudinaryApi {
        return Retrofit.Builder()
            .baseUrl("https://api.cloudinary.com/v1_1/${config.cloudName}/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CloudinaryApi::class.java)
    }
}

data class CloudinaryConfig(
    val cloudName: String,
    val uploadPreset: String
)


@Module
@InstallIn(SingletonComponent::class)
object ChatModule {

    @Provides
    @Singleton
    fun provideChatClient(): ChatClient = ChatClient.instance()
}