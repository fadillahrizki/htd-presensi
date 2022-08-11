package com.htd.presensi.models

data class Profile(
    var nama: String? = null,
    var nip: String? = null,
    var golongan: String? = null,
    var jabatan: String? = null,
    var instansi: String? = null,
    var atasan: String? = null,
    var namaAtasan: String? = null,
    var ponsel: String? = null,
    var email: String? = null,
)
