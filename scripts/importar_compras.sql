-- Importacao das compras (Assai/Sendas NFC-e 59645 e 59646). Sem balas.
-- Seguro para rodar mais de uma vez: ignora o que ja existe (pelo nome).
SET NAMES utf8mb4;
START TRANSACTION;
INSERT INTO rest_categorias (nome, ordem, ativa) SELECT 'Bebidas', 99, 1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_categorias WHERE LOWER(nome)='bebidas');

-- Yopro 250ml Cappuccino: 14 UN a R$ 9.4900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Yopro 250ml Cappuccino','UN',14,0,9.4900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Yopro 250ml Cappuccino');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',14,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Yopro 250ml Cappuccino' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Yopro 250ml Cappuccino',0,'BAR',1,1,0,(SELECT CASE WHEN EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE codigo_barras='7891025124252') THEN NULL ELSE '7891025124252' END) FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Yopro 250ml Cappuccino');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Yopro 250ml Cappuccino' WHERE c.nome='Yopro 250ml Cappuccino' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Yopro 250ml Baunilha: 34 UN a R$ 9.1900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Yopro 250ml Baunilha','UN',34,0,9.1900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Yopro 250ml Baunilha');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',34,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Yopro 250ml Baunilha' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Yopro 250ml Baunilha',0,'BAR',1,1,0,(SELECT CASE WHEN EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE codigo_barras='7891025124627') THEN NULL ELSE '7891025124627' END) FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Yopro 250ml Baunilha');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Yopro 250ml Baunilha' WHERE c.nome='Yopro 250ml Baunilha' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Yopro 250ml Morango 15g: 48 UN a R$ 9.1900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Yopro 250ml Morango 15g','UN',48,0,9.1900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Yopro 250ml Morango 15g');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',48,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Yopro 250ml Morango 15g' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Yopro 250ml Morango 15g',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Yopro 250ml Morango 15g');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Yopro 250ml Morango 15g' WHERE c.nome='Yopro 250ml Morango 15g' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Yopro 250ml Branco 15g: 24 UN a R$ 9.1900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Yopro 250ml Branco 15g','UN',24,0,9.1900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Yopro 250ml Branco 15g');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',24,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Yopro 250ml Branco 15g' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Yopro 250ml Branco 15g',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Yopro 250ml Branco 15g');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Yopro 250ml Branco 15g' WHERE c.nome='Yopro 250ml Branco 15g' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Biscoito Wafer Nutella 22g: 20 UN a R$ 4.7500
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Biscoito Wafer Nutella 22g','UN',20,0,4.7500,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Biscoito Wafer Nutella 22g');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',20,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Biscoito Wafer Nutella 22g' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');

-- Chocolate Kit Kat 41,5g Leite: 24 UN a R$ 5.0900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Chocolate Kit Kat 41,5g Leite','UN',24,0,5.0900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Chocolate Kit Kat 41,5g Leite');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',24,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Chocolate Kit Kat 41,5g Leite' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');

-- Choc Stick 25g Branco: 15 UN a R$ 1.9900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Choc Stick 25g Branco','UN',15,0,1.9900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Choc Stick 25g Branco');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',15,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Choc Stick 25g Branco' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');

-- Salgadinho Doritos 120g Nacho: 10 UN a R$ 14.3500
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Salgadinho Doritos 120g Nacho','UN',10,0,14.3500,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Salgadinho Doritos 120g Nacho');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',10,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Salgadinho Doritos 120g Nacho' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');

-- Batata Ruffles 68g Sal: 14 UN a R$ 7.8500
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Batata Ruffles 68g Sal','UN',14,0,7.8500,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Batata Ruffles 68g Sal');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',14,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Batata Ruffles 68g Sal' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');

-- Amendoim Japonês Elma 145g: 5 UN a R$ 7.3500
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Amendoim Japonês Elma 145g','UN',5,0,7.3500,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Amendoim Japonês Elma 145g');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',5,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Amendoim Japonês Elma 145g' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');

-- H2OH 500ml Limoneto: 24 UN a R$ 4.4900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'H2OH 500ml Limoneto','UN',24,0,4.4900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='H2OH 500ml Limoneto');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',24,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='H2OH 500ml Limoneto' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'H2OH 500ml Limoneto',0,'BAR',1,1,0,(SELECT CASE WHEN EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE codigo_barras='7892840812850') THEN NULL ELSE '7892840812850' END) FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='H2OH 500ml Limoneto');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='H2OH 500ml Limoneto' WHERE c.nome='H2OH 500ml Limoneto' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- H2OH 500ml Limão: 24 UN a R$ 4.4900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'H2OH 500ml Limão','UN',24,0,4.4900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='H2OH 500ml Limão');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',24,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='H2OH 500ml Limão' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'H2OH 500ml Limão',0,'BAR',1,1,0,(SELECT CASE WHEN EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE codigo_barras='7892840812423') THEN NULL ELSE '7892840812423' END) FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='H2OH 500ml Limão');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='H2OH 500ml Limão' WHERE c.nome='H2OH 500ml Limão' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Schweppes Citrus Lata 350ml: 30 UN a R$ 3.8900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Schweppes Citrus Lata 350ml','UN',30,0,3.8900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Schweppes Citrus Lata 350ml');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',30,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Schweppes Citrus Lata 350ml' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Schweppes Citrus Lata 350ml',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Schweppes Citrus Lata 350ml');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Schweppes Citrus Lata 350ml' WHERE c.nome='Schweppes Citrus Lata 350ml' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Gatorade 500ml Limão: 6 UN a R$ 5.8900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Gatorade 500ml Limão','UN',6,0,5.8900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Gatorade 500ml Limão');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',6,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Gatorade 500ml Limão' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Gatorade 500ml Limão',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Gatorade 500ml Limão');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Gatorade 500ml Limão' WHERE c.nome='Gatorade 500ml Limão' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Coca-Cola Lata 350ml: 60 UN a R$ 3.6900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Coca-Cola Lata 350ml','UN',60,0,3.6900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Coca-Cola Lata 350ml');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',60,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Coca-Cola Lata 350ml' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Coca-Cola Lata 350ml',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Coca-Cola Lata 350ml');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Coca-Cola Lata 350ml' WHERE c.nome='Coca-Cola Lata 350ml' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Coca-Cola Pack 6x350ml sem açúcar: 14 FARDO a R$ 22.4900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Coca-Cola Pack 6x350ml sem açúcar','FARDO',14,0,22.4900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Coca-Cola Pack 6x350ml sem açúcar');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',14,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Coca-Cola Pack 6x350ml sem açúcar' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');

-- Powerade 500ml Mountain Blast: 18 UN a R$ 6.1900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Powerade 500ml Mountain Blast','UN',18,0,6.1900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Powerade 500ml Mountain Blast');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',18,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Powerade 500ml Mountain Blast' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Powerade 500ml Mountain Blast',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Powerade 500ml Mountain Blast');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Powerade 500ml Mountain Blast' WHERE c.nome='Powerade 500ml Mountain Blast' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Monster 473ml Mango Loco: 6 UN a R$ 11.1900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Monster 473ml Mango Loco','UN',6,0,11.1900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Monster 473ml Mango Loco');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',6,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Monster 473ml Mango Loco' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Monster 473ml Mango Loco',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Monster 473ml Mango Loco');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Monster 473ml Mango Loco' WHERE c.nome='Monster 473ml Mango Loco' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Monster 473ml Tropical Thunder: 12 UN a R$ 11.1900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Monster 473ml Tropical Thunder','UN',12,0,11.1900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Monster 473ml Tropical Thunder');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',12,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Monster 473ml Tropical Thunder' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Monster 473ml Tropical Thunder',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Monster 473ml Tropical Thunder');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Monster 473ml Tropical Thunder' WHERE c.nome='Monster 473ml Tropical Thunder' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Água Viena c/ gás 510ml: 12 UN a R$ 1.5900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Água Viena c/ gás 510ml','UN',12,0,1.5900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Água Viena c/ gás 510ml');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',12,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Água Viena c/ gás 510ml' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Água Viena c/ gás 510ml',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Água Viena c/ gás 510ml');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Água Viena c/ gás 510ml' WHERE c.nome='Água Viena c/ gás 510ml' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Água Mar Doce c/ gás 500ml: 24 UN a R$ 1.5900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Água Mar Doce c/ gás 500ml','UN',24,0,1.5900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Água Mar Doce c/ gás 500ml');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',24,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Água Mar Doce c/ gás 500ml' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Água Mar Doce c/ gás 500ml',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Água Mar Doce c/ gás 500ml');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Água Mar Doce c/ gás 500ml' WHERE c.nome='Água Mar Doce c/ gás 500ml' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Cerveja Corona 330ml: 216 UN a R$ 7.6900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Cerveja Corona 330ml','UN',216,0,7.6900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Cerveja Corona 330ml');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',216,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Cerveja Corona 330ml' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Cerveja Corona 330ml',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Cerveja Corona 330ml');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Cerveja Corona 330ml' WHERE c.nome='Cerveja Corona 330ml' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Cerveja Heineken 330ml: 408 UN a R$ 6.3900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Cerveja Heineken 330ml','UN',408,0,6.3900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Cerveja Heineken 330ml');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',408,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Cerveja Heineken 330ml' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Cerveja Heineken 330ml',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Cerveja Heineken 330ml');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Cerveja Heineken 330ml' WHERE c.nome='Cerveja Heineken 330ml' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Cerveja Stella Artois Pure Gold 330ml: 216 UN a R$ 7.2900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Cerveja Stella Artois Pure Gold 330ml','UN',216,0,7.2900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Cerveja Stella Artois Pure Gold 330ml');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',216,'Compra inicial - NFC-e 59645',NOW() FROM rest_insumos i WHERE i.nome='Cerveja Stella Artois Pure Gold 330ml' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59645');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Cerveja Stella Artois Pure Gold 330ml',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Cerveja Stella Artois Pure Gold 330ml');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Cerveja Stella Artois Pure Gold 330ml' WHERE c.nome='Cerveja Stella Artois Pure Gold 330ml' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Red Bull 250ml: 96 UN a R$ 8.4900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Red Bull 250ml','UN',96,0,8.4900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Red Bull 250ml');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',96,'Compra inicial - NFC-e 59646',NOW() FROM rest_insumos i WHERE i.nome='Red Bull 250ml' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59646');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Red Bull 250ml',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Red Bull 250ml');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Red Bull 250ml' WHERE c.nome='Red Bull 250ml' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Red Bull 250ml Cereja: 24 UN a R$ 8.4900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Red Bull 250ml Cereja','UN',24,0,8.4900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Red Bull 250ml Cereja');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',24,'Compra inicial - NFC-e 59646',NOW() FROM rest_insumos i WHERE i.nome='Red Bull 250ml Cereja' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59646');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Red Bull 250ml Cereja',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Red Bull 250ml Cereja');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Red Bull 250ml Cereja' WHERE c.nome='Red Bull 250ml Cereja' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Cabaré Ice 275ml Limão: 24 UN a R$ 7.3900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Cabaré Ice 275ml Limão','UN',24,0,7.3900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Cabaré Ice 275ml Limão');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',24,'Compra inicial - NFC-e 59646',NOW() FROM rest_insumos i WHERE i.nome='Cabaré Ice 275ml Limão' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59646');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Cabaré Ice 275ml Limão',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Cabaré Ice 275ml Limão');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Cabaré Ice 275ml Limão' WHERE c.nome='Cabaré Ice 275ml Limão' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Cabaré Ice 275ml Frutas Vermelhas: 12 UN a R$ 7.3900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Cabaré Ice 275ml Frutas Vermelhas','UN',12,0,7.3900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Cabaré Ice 275ml Frutas Vermelhas');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',12,'Compra inicial - NFC-e 59646',NOW() FROM rest_insumos i WHERE i.nome='Cabaré Ice 275ml Frutas Vermelhas' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59646');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Cabaré Ice 275ml Frutas Vermelhas',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Cabaré Ice 275ml Frutas Vermelhas');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Cabaré Ice 275ml Frutas Vermelhas' WHERE c.nome='Cabaré Ice 275ml Frutas Vermelhas' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Skol Beats GT 269ml: 48 UN a R$ 8.2900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Skol Beats GT 269ml','UN',48,0,8.2900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Skol Beats GT 269ml');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',48,'Compra inicial - NFC-e 59646',NOW() FROM rest_insumos i WHERE i.nome='Skol Beats GT 269ml' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59646');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Skol Beats GT 269ml',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Skol Beats GT 269ml');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Skol Beats GT 269ml' WHERE c.nome='Skol Beats GT 269ml' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

-- Red Bull 250ml Melancia: 24 UN a R$ 8.4900
INSERT INTO rest_insumos (nome, unidade, estoque_atual, estoque_minimo, custo_unitario, ativo) SELECT 'Red Bull 250ml Melancia','UN',24,0,8.4900,1 FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_insumos WHERE nome='Red Bull 250ml Melancia');
INSERT INTO rest_movimentos_insumo (insumo_id, tipo, quantidade, observacao, criado_em) SELECT i.id,'ENTRADA',24,'Compra inicial - NFC-e 59646',NOW() FROM rest_insumos i WHERE i.nome='Red Bull 250ml Melancia' AND NOT EXISTS (SELECT 1 FROM rest_movimentos_insumo m WHERE m.insumo_id=i.id AND m.observacao='Compra inicial - NFC-e 59646');
INSERT INTO rest_itens_cardapio (categoria_id, nome, preco, praca, ativo, pausado, ordem, codigo_barras) SELECT (SELECT id FROM rest_categorias WHERE LOWER(nome)='bebidas' ORDER BY id LIMIT 1),'Red Bull 250ml Melancia',0,'BAR',1,1,0,NULL FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM rest_itens_cardapio WHERE nome='Red Bull 250ml Melancia');
INSERT INTO rest_ficha_tecnica (item_id, insumo_id, quantidade) SELECT c.id, i.id, 1 FROM rest_itens_cardapio c JOIN rest_insumos i ON i.nome='Red Bull 250ml Melancia' WHERE c.nome='Red Bull 250ml Melancia' AND NOT EXISTS (SELECT 1 FROM rest_ficha_tecnica f WHERE f.item_id=c.id);

COMMIT;