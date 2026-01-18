package edu.ucam.reservashack.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import edu.ucam.reservashack.data.remote.SessionCookieJar
import edu.ucam.reservashack.data.remote.TakeASpotApi
import edu.ucam.reservashack.data.remote.XSRFInterceptor
import edu.ucam.reservashack.domain.repository.SessionRepository
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(
        sessionRepository: SessionRepository
    ): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            // Level.BASIC: solo loguea URLs y códigos de respuesta, sin exponer datos sensibles
            // Es mucho más seguro que Level.BODY que expone cookies y tokens
            level = HttpLoggingInterceptor.Level.BASIC
        }

        return OkHttpClient.Builder()
            .cookieJar(SessionCookieJar(sessionRepository))
            .addInterceptor(XSRFInterceptor(sessionRepository))
            .addInterceptor(logging)
            // Timeouts recomendados para evitar bloqueos indefinidos
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideTakeASpotApi(client: OkHttpClient): TakeASpotApi {
        return Retrofit.Builder()
            .baseUrl("https://reservas.ucam.edu/") // URL Base
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TakeASpotApi::class.java)
    }
}