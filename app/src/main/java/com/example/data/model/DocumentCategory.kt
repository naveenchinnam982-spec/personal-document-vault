package com.example.data.model

data class CategoryInfo(
    val name: String,
    val icon: String,
    val description: String
)

object DocumentCategories {
    val ALL = listOf(
        CategoryInfo("Identity", "badge", "Aadhaar, PAN, Passport, Driving License, Voter ID"),
        CategoryInfo("Education", "school", "Degree, 10th & 12th, Marksheets, Diplomas"),
        CategoryInfo("Finance", "account_balance", "Bank Statements, Tax Returns, Investments"),
        CategoryInfo("Employment", "work", "Offer Letters, Payslips, Experience Letters"),
        CategoryInfo("Vehicle", "directions_car", "RC, Vehicle Insurance, Pollution, PUC"),
        CategoryInfo("Insurance", "security", "Health, Life, Home, Travel Policies"),
        CategoryInfo("Medical", "local_hospital", "Prescriptions, Lab Reports, Vaccine Records"),
        CategoryInfo("Certificates", "workspace_premium", "Course Certs, Awards, Recognitions"),
        CategoryInfo("Personal", "person", "Personal letters, contracts, private notes"),
        CategoryInfo("Other", "folder", "General & Miscellaneous Documents")
    )

    val DEFAULT_NAMES = ALL.map { it.name }
}
