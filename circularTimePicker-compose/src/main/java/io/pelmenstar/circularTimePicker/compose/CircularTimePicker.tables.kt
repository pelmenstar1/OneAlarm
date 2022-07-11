package io.pelmenstar.circularTimePicker.compose

import io.pelmenstar.onealarm.shared.FloatPair

// A table which is comprised with coefficents drawing hour label in a clock.
internal val X_Y_FRACTION_TABLE_FOR_LABELS = longArrayOf(
    FloatPair.create(0f, -1f),
    FloatPair.create(1f, 0f),
    FloatPair.create(0f, 1f),
    FloatPair.create(-1f, 0f)
)

// This table is generated by genTrigTable tool in root of the project.
//
// It contains packed pair of sine (lowest 32 bits) and cosine (highest 32 bits) values.
// Angle as an argument for the functions is computed as index in the array multiplied by 3.
// Which means it's actually an arithmetical progression whose initial term is 0 and difference is 3
internal val SIN_COS_FOR_TICKS = longArrayOf(
    4575657221408423936L,
    4575558468254457402L,
    4575262484886590213L,
    4574770075480895579L,
    4574082592954181325L,
    4573201918503191534L,
    4572130474488379258L,
    4570871185783880705L,
    4569427514133331913L,
    4567803415201608049L,
    4566003334279856128L,
    4564032210579713399L,
    4561895447170808088L,
    4559598898093562660L,
    4557148855475063845L,
    4554552040939128051L,
    4551815571246563005L,
    4548946941115298570L,
    4545954018925419453L,
    4542845008064459549L,
    4539628425452565463L,
    4532997756157171969L,
    4526187759092227613L,
    4519217095890632480L,
    4512104874861885553L,
    4504870586567575274L,
    4491468497602897890L,
    4476631198071576869L,
    4455769794590710013L,
    4419823688577164847L,
    2633815448965087232L,
    -4803548348277610961L,
    -4767602242264065795L,
    -4746740838783198939L,
    -4731903539251877918L,
    -4718501450287200534L,
    -4711267161992890255L,
    -4704154940964143328L,
    -4697184277762548195L,
    -4690374280697603839L,
    -4683743611402210345L,
    -4680527028790316259L,
    -4677418017929356355L,
    -4674425095739477238L,
    -4671556465608212803L,
    -4668819995915647757L,
    -4666223181379711963L,
    -4663773138761213148L,
    -4661476589683967720L,
    -4659339826275062409L,
    -4657368702574919680L,
    -4655568621653167759L,
    -4653944522721443895L,
    -4652500851070895103L,
    -4651241562366396550L,
    -4650170118351584274L,
    -4649289443900594483L,
    -4648601961373880229L,
    -4648109551968185595L,
    -4647813568600318406L,
    -4647714814824730318L,
    -4647813566452834758L,
    -4648109549820701947L,
    -4648601959226396581L,
    -4649289441753110835L,
    -4650170116204100626L,
    -4651241560218912902L,
    -4652500848923411455L,
    -4653944520573960247L,
    -4655568619505684111L,
    -4657368700427436032L,
    -4659339824127578761L,
    -4661476587536484072L,
    -4663773136613729500L,
    -4666223179232228315L,
    -4668819993768164109L,
    -4671556463460729155L,
    -4674425093591993590L,
    -4677418015781872707L,
    -4680527026642832611L,
    -4683743609254726697L,
    -4690374278550120191L,
    -4697184275615064547L,
    -4704154938816659680L,
    -4711267159845406607L,
    -4718501448139716886L,
    -4731903537104394270L,
    -4746740836635715291L,
    -4767602240116582147L,
    -4803548346130127313L,
    -6533656761751044096L,
    4419823690724648495L,
    4455769796738193661L,
    4476631200219060517L,
    4491468499750381538L,
    4504870588715058922L,
    4512104877009369201L,
    4519217098038116128L,
    4526187761239711261L,
    4532997758304655617L,
    4539628427600049111L,
    4542845010211943197L,
    4545954021072903101L,
    4548946943262782218L,
    4551815573394046653L,
    4554552043086611699L,
    4557148857622547493L,
    4559598900241046308L,
    4561895449318291736L,
    4564032212727197047L,
    4566003336427339776L,
    4567803417349091697L,
    4569427516280815561L,
    4570871187931364353L,
    4572130476635862906L,
    4573201920650675182L,
    4574082595101664973L,
    4574770077628379227L,
    4575262487034073861L,
    4575558470401941050L,
    4575657224185917746L,
)