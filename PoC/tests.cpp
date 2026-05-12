#include <bits/stdc++.h>

using namespace std;

// --- ESTRUCTURAS DE DATOS ---
struct Horario {
    int dia;
    int inicio;
    int fin;
};

struct Paralelo {
    int id;
    vector<Horario> horarios;
};

struct Materia {
    int id;
    vector<Paralelo> paralelos;
};

struct Preferencias {
    int peso_manana_tarde;
    bool prefiere_manana; 
    int peso_puentes_cortos;
    bool prefiere_puentes_cortos;
    int peso_max_por_dia; 
    int max_materias_por_dia;
    int peso_paralelos_pref;
    map<int, int> paralelos_preferidos;
    int cant_materias_tomar;
    map<int, int> materia_paralelo_obligatorio; 
};

struct ResultadoHorario {
    vector<pair<Materia, Paralelo>> seleccion;
    int puntaje;

    bool operator<(const ResultadoHorario& otro) const {
        return puntaje > otro.puntaje;
    }
};

// --- VARIABLES GLOBALES ---
map<int, vector<int>> grafo_prerrequisitos; 
map<int, int> grados_entrada_global; 

// --- FUNCIONES AUXILIARES ---
bool hayCruce(const vector<Horario>& h1, const vector<Horario>& h2) {
    for (const auto& a : h1) {
        for (const auto& b : h2) {
            if (a.dia == b.dia) {
                if (a.inicio < b.fin && b.inicio < a.fin) return true;
            }
        }
    }
    return false;
}

int evaluarHorario(const vector<pair<Materia, Paralelo>>& seleccion, const Preferencias& pref) {
    int puntaje = 0;
    map<int, int> materias_por_dia;
    map<int, vector<pair<int, int>>> horas_por_dia;

    for (const auto& item : seleccion) {
        int mat_id = item.first.id;
        int par_id = item.second.id;

        if (pref.paralelos_preferidos.count(mat_id) && pref.paralelos_preferidos.at(mat_id) == par_id) {
            puntaje += pref.peso_paralelos_pref * 15;
        }

        for (const auto& h : item.second.horarios) {
            materias_por_dia[h.dia]++;
            horas_por_dia[h.dia].push_back({h.inicio, h.fin});
            
            bool es_manana = h.inicio < 13;
            if (es_manana == pref.prefiere_manana) puntaje += pref.peso_manana_tarde * 10; 
        }
    }

    for (auto const& [dia, count] : materias_por_dia) {
        if (count > pref.max_materias_por_dia) puntaje -= pref.peso_max_por_dia * 50; 
        else puntaje += pref.peso_max_por_dia * 10;
    }

    for (auto& [dia, horas] : horas_por_dia) {
        if (horas.size() > 1) {
            sort(horas.begin(), horas.end());
            for (size_t i = 0; i < horas.size() - 1; i++) {
                int puente = horas[i+1].first - horas[i].second;
                if (pref.prefiere_puentes_cortos) {
                    if (puente == 0) puntaje += pref.peso_puentes_cortos * 20;
                    else if (puente <= 2) puntaje += pref.peso_puentes_cortos * 10;
                    else puntaje -= pref.peso_puentes_cortos * 5; 
                }
            }
        }
    }
    return puntaje;
}

vector<vector<int>> proyectarMaterias(const vector<int>& materias_aprobadas_ahora) {
    vector<vector<int>> niveles;
    map<int, int> grados = grados_entrada_global;
    queue<int> q;

    for (int m : materias_aprobadas_ahora) {
        for (int v : grafo_prerrequisitos[m]) {
            grados[v]--;
            if (grados[v] == 0) q.push(v);
        }
    }

    while (!q.empty()) {
        int size = q.size();
        vector<int> nivel_actual;
        for (int i = 0; i < size; i++) {
            int u = q.front();
            q.pop();
            nivel_actual.push_back(u);
            
            for (int v : grafo_prerrequisitos[u]) {
                grados[v]--;
                if (grados[v] == 0) q.push(v);
            }
        }
        if (!nivel_actual.empty()) niveles.push_back(nivel_actual);
    }
    return niveles;
}

void generarHorarios(int idx_materia, vector<Materia>& materias_disponibles, 
                     vector<pair<Materia, Paralelo>>& seleccion_actual, 
                     Preferencias& pref, vector<ResultadoHorario>& resultados) {
    
    if (seleccion_actual.size() == pref.cant_materias_tomar) {
        ResultadoHorario res;
        res.seleccion = seleccion_actual; 
        res.puntaje = evaluarHorario(seleccion_actual, pref);
        resultados.push_back(res);
        return;
    }

    if (idx_materia >= materias_disponibles.size()) return;

    Materia& mat_actual = materias_disponibles[idx_materia];
    generarHorarios(idx_materia + 1, materias_disponibles, seleccion_actual, pref, resultados);

    for (Paralelo& p : mat_actual.paralelos) {
        if (pref.materia_paralelo_obligatorio.count(mat_actual.id) && 
            pref.materia_paralelo_obligatorio[mat_actual.id] != p.id) {
            continue;
        }

        bool cruce = false;
        for (auto& sel : seleccion_actual) {
            if (hayCruce(sel.second.horarios, p.horarios)) {
                cruce = true; break;
            }
        }

        if (!cruce) {
            seleccion_actual.push_back({mat_actual, p});
            generarHorarios(idx_materia + 1, materias_disponibles, seleccion_actual, pref, resultados);
            seleccion_actual.pop_back(); 
        }
    }
}
void test_cruces() {
    cout << "Corriendo Test: Cruces de Horario..." << endl;
    vector<Horario> grupo_a = {{0, 8, 10}, {2, 8, 10}}; // Lunes y Miercoles de 8 a 10
    vector<Horario> grupo_b = {{0, 9, 11}};             // Lunes de 9 a 11 (Cruce con A)
    vector<Horario> grupo_c = {{0, 10, 12}};            // Lunes de 10 a 12 (Sin cruce con A)

    assert(hayCruce(grupo_a, grupo_b) == true);
    assert(hayCruce(grupo_a, grupo_c) == false);
    cout << "Test de Cruces: OK\n" << endl;
}

void test_bfs_proyeccion() {
    cout << "Corriendo Test: Proyeccion BFS..." << endl;
    
    // Configurar grafo mockeado global
    grafo_prerrequisitos.clear();
    grados_entrada_global.clear();

    grafo_prerrequisitos[101] = {201, 202};
    grafo_prerrequisitos[201] = {301}; 
    
    grados_entrada_global[201] = 1;
    grados_entrada_global[202] = 1;
    grados_entrada_global[301] = 1;

    // Simulamos aprobar la materia 101
    vector<int> aprobadas = {101};
    vector<vector<int>> proyeccion = proyectarMaterias(aprobadas);

    // Verificamos niveles generados
    assert(proyeccion.size() == 2); // Deberia desbloquear 2 semestres adelante (2xx y luego 3xx)
    
    // Verificamos nivel 1 (Semestre siguiente)
    bool tiene_201 = false, tiene_202 = false;
    for(int mat : proyeccion[0]){
        if(mat == 201) tiene_201 = true;
        if(mat == 202) tiene_202 = true;
    }
    assert(tiene_201 && tiene_202);

    // Verificamos nivel 2 (Subsiguiente)
    assert(proyeccion[1].size() == 1 && proyeccion[1][0] == 301);

    cout << "Test Proyeccion BFS: OK\n" << endl;
}

void test_generacion_y_restricciones() {
    cout << "Corriendo Test: Generacion con Restricciones Estrictas..." << endl;
    
    Materia m1 = {101, {{1, {{0, 8, 10}}}}};
    Materia m2 = {102, {{1, {{0, 8, 10}}}}}; // Cruza exactamente con M1
    Materia m3 = {103, {{1, {{1, 10, 12}}}}};
    vector<Materia> disponibles = {m1, m2, m3};

    Preferencias pref;
    pref.cant_materias_tomar = 2;
    // Preferencias basicas para que no falle el iterador
    pref.peso_manana_tarde = 0; pref.peso_puentes_cortos = 0; pref.peso_max_por_dia = 0; pref.peso_paralelos_pref = 0;
    pref.max_materias_por_dia = 5;

    vector<ResultadoHorario> resultados;
    vector<pair<Materia, Paralelo>> seleccion;

    generarHorarios(0, disponibles, seleccion, pref, resultados);

    // No deberia existir ninguna opcion que tome M1 y M2 al mismo tiempo
    for(auto& res : resultados) {
        bool tiene_m1 = false, tiene_m2 = false;
        for(auto& par : res.seleccion) {
            if(par.first.id == 101) tiene_m1 = true;
            if(par.first.id == 102) tiene_m2 = true;
        }
        assert(!(tiene_m1 && tiene_m2)); 
    }
    cout << "Test Generacion con Restricciones: OK\n" << endl;
}

int main() {
    cout << "--- INICIANDO PRUEBAS UNITARIAS ---" << endl;
    test_cruces();
    test_bfs_proyeccion();
    test_generacion_y_restricciones();
    cout << "--- TODAS LAS PRUEBAS PASARON EXITOSAMENTE ---" << endl;
    return 0;
}